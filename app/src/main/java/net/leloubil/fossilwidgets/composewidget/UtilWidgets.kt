package net.leloubil.fossilwidgets.composewidget

import android.util.Log
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import net.leloubil.fossilwidgets.widgets.WidgetContent
import net.leloubil.fossilwidgets.widgetsapi.WidgetComposeState
import net.leloubil.fossilwidgets.widgetsapi.makeProvider
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun WidgetComposeState.TextWidget(top: String, bottom: String) = makeProvider {
    MutableStateFlow(WidgetContent(top, bottom))
}

fun WidgetComposeState.LineText(text: String) = MutableStateFlow(text)
suspend fun WidgetComposeState.Scrollable(
    sourceProviderState: StateFlow<String>,
    enabled: Boolean = true,
    windowSize: Int = 20,
    charsPerScroll: Int = 8,
    scrollDuration: Duration = 3.seconds,
    scrollWaitAtStart: Duration = 5.seconds,
    scrollWaitAtEnd: Duration = 3.seconds
): Flow<String> = channelFlow {
    sourceProviderState.useState { data ->
        Log.i("Scrollable", "data: $data")
        send(data.substring(0, min(windowSize, data.length)))
        if (!enabled) {
            awaitClose()
        }
        var topScroll = 0
        while (currentCoroutineContext().isActive) {
            if (topScroll == 0) {
                Log.i("Scrollable", "Waiting at start")
                delay(scrollWaitAtStart)
                if (data.length > windowSize) {
                    topScroll += charsPerScroll
                }
            } else if (topScroll + windowSize >= data.length) {
                Log.i("Scrollable", "Waiting at end")
                delay(scrollWaitAtEnd)
                topScroll = 0
            } else {
                Log.i("Scrollable", "Scrolling")
                delay(scrollDuration)
                topScroll += charsPerScroll
            }
            val lower = min(topScroll, data.length)
            val upper = min(topScroll + windowSize, data.length)
            send(data.substring(lower, upper))
        }
    }
}.stateIn(coroutineScope)

fun WidgetComposeState.DifferingTopBottom(
    topAction: Flow<String>,
    bottomAction: Flow<String>
): Flow<WidgetContent> = makeProvider {
    topAction.combine(bottomAction) { top, bottom ->
        WidgetContent(top, bottom)
    }
}