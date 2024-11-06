package net.leloubil.fossilwidgets.api

import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import net.leloubil.fossilwidgets.widgets.WidgetContent
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun WidgetsApiContext.Scrollable(
    enabled: Boolean = true,
    windowSize: Int = 20,
    charsPerScroll: Int = 8,
    scrollDuration: Duration = 3.seconds,
    scrollWaitAtStart: Duration = 2.seconds,
    scrollWaitAtEnd: Duration = 3.seconds,
    it: StringProviderCreator
) =
    Scrollable(
        makeStringProvider(it),
        enabled,
        windowSize,
        charsPerScroll,
        scrollDuration,
        scrollWaitAtStart,
        scrollWaitAtEnd
    )

//fun WidgetsApiContext.Scrollable(
//    sourceProviderState: String,
//    enabled: Boolean = true,
//    windowSize: Int = 20,
//    charsPerScroll: Int = 8,
//    scrollDuration: Duration = 3.seconds,
//    scrollWaitAtStart: Duration = 2.seconds,
//    scrollWaitAtEnd: Duration = 3.seconds
//) = Scrollable(
//    sourceProviderState.static(),
//    enabled,
//    windowSize,
//    charsPerScroll,
//    scrollDuration,
//    scrollWaitAtStart,
//    scrollWaitAtEnd
//)

private fun Scrollable(
    sourceProviderState: StringProvider,
    enabled: Boolean = true,
    windowSize: Int = 20,
    charsPerScroll: Int = 8,
    scrollDuration: Duration = 3.seconds,
    scrollWaitAtStart: Duration = 3.seconds,
    scrollWaitAtEnd: Duration = 3.seconds
) = makeStringProvider {
    Log.i("Scrollable", "Creating ScrollingText")
    val data by useState { sourceProviderState.toState() }
    if (data.length < windowSize) {
        return@makeStringProvider data.static()
    }
    if (!enabled) {
        return@makeStringProvider data.substring(0, min(windowSize, data.length)).static()
    }
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
        Log.i("ScrollingText", "Emitting text: ${text.substring(lower, upper)}")
        emit(text.substring(lower, upper))
    }
}

fun WidgetsApiContext.Text(
    topText: StringProvider,
    bottomText: StringProvider,
) = makeWidget {
    Log.i("Text", "Creating StaticContent")
    @Suppress("NAME_SHADOWING")
    Log.i("Text", "Rendering topText")
    val topText by useState { topText.toState() }
    Log.i("Text", "TopText: $topText")

    @Suppress("NAME_SHADOWING")
    Log.i("Text", "Rendering bottomText")
    val bottomText by useState { bottomText.toState() }
    Log.i("Text", "BottomText: $bottomText")

    StaticContent(topText, bottomText)
}

// region alternative-text
fun WidgetsApiContext.Text(
    topText: String,
    bottomText: StringProvider
) = Text(topText.static(), bottomText)

fun WidgetsApiContext.Text(
    topText: StringProvider,
    bottomText: String
) = Text(topText, bottomText.static())

fun WidgetsApiContext.Text(
    topText: String,
    bottomText: String
) = Text(topText.static(), bottomText.static())
// endregion


fun WidgetsApiContext.StaticContent(
    topText: String,
    bottomText: String
): WidgetProvider =
    {
        Log.i("StaticContent", "Creating WidgetContent")
        Log.i("StaticContent", "TopText: $topText")
        Log.i("StaticContent", "BottomText: $bottomText")
        MutableStateFlow(WidgetContent(topText, bottomText))
    }
