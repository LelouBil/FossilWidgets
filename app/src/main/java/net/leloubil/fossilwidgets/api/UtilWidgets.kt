package net.leloubil.fossilwidgets.api

import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import net.leloubil.fossilwidgets.widgets.WidgetContent
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun CompositionContext<WidgetContent>.Scrollable(
    enabled: Boolean = true,
    windowSize: Int = 14,
    charsPerScroll: Int = 8,
    scrollDuration: Duration = 3.seconds,
    scrollWaitAtStart: Duration = 2.seconds,
    scrollWaitAtEnd: Duration = 3.seconds,
    it: ProviderCreator<String>
) =
    Scrollable(
        makeReactive<String>(it),
        enabled,
        windowSize,
        charsPerScroll,
        scrollDuration,
        scrollWaitAtStart,
        scrollWaitAtEnd
    )

private fun Scrollable(
    sourceProviderState: Provider<String>,
    enabled: Boolean = true,
    windowSize: Int = 14,
    charsPerScroll: Int = 8,
    scrollDuration: Duration = 3.seconds,
    scrollWaitAtStart: Duration = 3.seconds,
    scrollWaitAtEnd: Duration = 3.seconds
) = makeReactive<String>{
    Log.i("Scrollable", "Creating ScrollingText")
    val data by useState { sourceProviderState.toState() }
    if (data.length < windowSize) {
        data.static()
    }
    else if (!enabled) {
        data.substring(0, min(windowSize, data.length)).let {
            if(data.length > windowSize) "$it…" else it
        }.static()
    } else {
        Log.i("Scrollable", "Data: $data");
        {
            Log.i("Scrollable", "coroutineScope: $coroutineScope")
            ScrollingText(
                data,
                windowSize,
                charsPerScroll,
                scrollDuration,
                scrollWaitAtStart,
                scrollWaitAtEnd
            ).stateIn(
                coroutineScope,
                SharingStarted.Lazily,
                data.substring(0, min(windowSize, data.length))
            )
        }
    }
}

private fun ScrollingText(
    text: String,
    windowSize: Int,
    charsPerScroll: Int,
    scrollDuration: Duration,
    scrollWaitAtStart: Duration,
    scrollWaitAtEnd: Duration
): Flow<String> = flow {
    Log.i("ScrollingText", "Starting ScrollingText")
    var topScroll = 0
    while (currentCoroutineContext().isActive) {
        if (topScroll == 0) {
            delay(scrollWaitAtStart)
            if (text.length > windowSize) {
                topScroll += charsPerScroll
            }
        } else if (topScroll + windowSize >= text.length) {
            delay(scrollWaitAtEnd)
            topScroll = 0
        } else {
            delay(scrollDuration)
            topScroll += charsPerScroll
        }
        val lower = min(topScroll, text.length)
        val upper = min(topScroll + windowSize, text.length)
        val dots = if (upper < text.length) "…" else ""
        Log.i("ScrollingText", "Emitting text: ${text.substring(lower, upper)}$dots")
        emit(text.substring(lower, upper) + dots)
    }
}

fun CompositionContext<WidgetContent>.Text(
    topText: Provider<String>,
    bottomText: Provider<String>
) = makeReactive<WidgetContent>{
    Log.i("Text", "Creating StaticContent")
    Log.i("Text", "Rendering topText")
    @Suppress("NAME_SHADOWING")
    val topText by useState { topText.toState() }
    Log.i("Text", "TopText: $topText")

    Log.i("Text", "Rendering bottomText")
    @Suppress("NAME_SHADOWING")
    val bottomText by useState { bottomText.toState() }
    Log.i("Text", "BottomText: $bottomText")
    WidgetContent(topText, bottomText).static()
}

// region alternative-text
fun CompositionContext<WidgetContent>.Text(
    topText: String,
    bottomText: CompositionContext<String>.() -> Flow<String>
) = Text(topText.static(), bottomText)

fun CompositionContext<WidgetContent>.Text(
    topText: CompositionContext<String>.() -> Flow<String>,
    bottomText: String
) = Text(topText, bottomText.static())

fun CompositionContext<WidgetContent>.Text(
    topText: String,
    bottomText: String
) = Text(topText.static(), bottomText.static())
// endregion

