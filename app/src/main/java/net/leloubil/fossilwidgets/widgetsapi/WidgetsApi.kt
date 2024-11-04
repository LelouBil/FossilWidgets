package net.leloubil.fossilwidgets.widgetsapi

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.leloubil.fossilwidgets.widgets.WidgetContent
import java.lang.Thread.State
import kotlinx.coroutines.flow.StateFlow as SF


typealias WidgetContentProvider = Flow<WidgetContent>
typealias WidgetContentProviderFunc = suspend WidgetComposeState.() -> WidgetContentProvider
typealias WatchFaceProvider = WidgetComposeState.() -> Flow<WatchFace>

data class WatchFace(val name: String, val widgets: List<WidgetContentProvider>)

fun WidgetComposeState.makeProvider(
    action: suspend WidgetComposeState.() -> Flow<WidgetContent>
): WidgetContentProvider =
    channelFlow {
        with(WidgetComposeState(this@channelFlow, context)) {
            action().collectLatest {
                send(it)
            }
        }
    }

fun WidgetComposeState.makeWatchface(
    action: suspend WidgetComposeState.() -> Flow<WatchFace>
): Flow<WatchFace> =
    channelFlow {
        with(WidgetComposeState(this@channelFlow, context)) {
            action().collectLatest {
                send(it)
            }
        }
    }


class WidgetDataProviderState<T>(
    private val producerScope: ProducerScope<T>,
    val coroutineScope: CoroutineScope,
    val context: Context
) {
    suspend fun emit(content: T) {
        producerScope.send(content)
    }
}


suspend fun <T> WidgetComposeState.widgetDataProvider(
    action: suspend WidgetDataProviderState<T>.() -> Unit
): StateFlow<T> =
    channelFlow {
        val receiver = WidgetDataProviderState<T>(this@channelFlow, coroutineScope, context)
        receiver.action()
    }.stateIn(coroutineScope)


open class WidgetComposeState(
    val coroutineScope: CoroutineScope,
    val context: Context
) {


    suspend fun <T, R> Flow<T>.useStateW(
        action: suspend WidgetComposeState.(T) -> Flow<R>
    ): StateFlow<R> = channelFlow {
        collectLatest { cur ->
            coroutineScope {
                WidgetComposeState(this@coroutineScope, context).action(cur).collectLatest { new ->
                    send(new)
                }
            }

        }
    }.stateIn(coroutineScope)




    suspend fun <T, R> Flow<T>.useState(
        action: suspend WidgetComposeState.(T) -> R
    ): StateFlow<R> = channelFlow {
        collectLatest { cur ->
            coroutineScope {
                send(WidgetComposeState(this@coroutineScope, context).action(cur))
            }
        }
    }.stateIn(coroutineScope)


}