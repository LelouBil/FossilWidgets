package net.leloubil.fossilwidgets.stateproviders

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import net.leloubil.fossilwidgets.widgetsapi.WidgetComposeState

sealed class MediaInfoUpdate {
    data class MetadataChanged(val mediaInfo: MediaMetadata?) : MediaInfoUpdate()
    data class PlaybackStateChanged(val isPlaying: Boolean) : MediaInfoUpdate()
}

data class MediaMetadata(val title: String, val artist: String)
data class MediaState(val metadata: MediaMetadata?, val isPlaying: Boolean)

fun android.media.MediaMetadata?.convert(): MediaMetadata? {
    return if (this == null) null else MediaMetadata(
        this.getString("android.media.metadata.TITLE") ?: "",
        this.getString("android.media.metadata.ARTIST") ?: ""
    )
}

fun mediaSessionFlowCallback(
    mediaSession: MediaController
) = callbackFlow {
    val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            super.onMetadataChanged(metadata)
            trySend(MediaInfoUpdate.MetadataChanged(metadata.convert()))
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            if (state?.state == PlaybackState.STATE_PLAYING) {
                Log.d("MediaWidgetContentProvider", "Media is playing")
                trySend(MediaInfoUpdate.PlaybackStateChanged(true))
            } else {
                trySend(MediaInfoUpdate.PlaybackStateChanged(false))
            }
        }

        override fun onSessionDestroyed() {
            Log.d("MediaWidgetContentProvider", "Session destroyed callback")
            mediaSession.unregisterCallback(this)
        }
    }
    mediaSession.registerCallback(callback)
    awaitClose {
        mediaSession.unregisterCallback(callback)
    }
}

fun mediaSessionStateFlow(
    mediaSession: MediaController,
    coroutineScope: CoroutineScope
): StateFlow<MediaState> {
    var mediaState = MediaState(
        mediaSession.metadata.convert(),
        mediaSession.playbackState?.state == PlaybackState.STATE_PLAYING
    )
    return flow {
        mediaSessionFlowCallback(mediaSession).collect {
            mediaState = when (it) {
                is MediaInfoUpdate.MetadataChanged -> {
                    mediaState.copy(metadata = it.mediaInfo)
                }

                is MediaInfoUpdate.PlaybackStateChanged -> {
                    mediaState.copy(isPlaying = it.isPlaying)
                }
            }
            emit(mediaState)
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, mediaState)
}

fun mediaSessionCallbackFlow(context: Context) = callbackFlow {
    val (listenerComponent, mediaSessionManager) = mediaSessionManager(context)
    val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        controllers?.forEach {
            trySend(it)
        }
    }
    mediaSessionManager.addOnActiveSessionsChangedListener(listener, listenerComponent)
    awaitClose {
        mediaSessionManager.removeOnActiveSessionsChangedListener(listener)
    }
}

private fun mediaSessionManager(context: Context): Pair<ComponentName, MediaSessionManager> {
    val listenerComponent =
        ComponentName(context, NotificationListener::class.java)
    val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    return Pair(listenerComponent, mediaSessionManager)
}

fun mediaSessionStateFlow(
    context: Context,
    coroutineScope: CoroutineScope
): StateFlow<MediaController?> {
    val (listener, currentMediaSession) = mediaSessionManager(context)
    return mediaSessionCallbackFlow(context).stateIn(
        coroutineScope, SharingStarted.Eagerly,
        currentMediaSession.getActiveSessions(listener).firstOrNull()
    )
}

suspend fun WidgetComposeState.getMediaStateFlow(): StateFlow<MediaState> = channelFlow {
    mediaSessionStateFlow(context, coroutineScope).collectLatest { it ->
        if (it == null) {
            send(MediaState(null, false))
        } else {
            mediaSessionStateFlow(it, coroutineScope).collectLatest { state ->
                send(state)
            }
        }
    }
}.stateIn(coroutineScope)