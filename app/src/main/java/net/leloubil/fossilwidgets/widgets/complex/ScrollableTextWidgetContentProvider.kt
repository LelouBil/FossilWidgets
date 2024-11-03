package net.leloubil.fossilwidgets.widgets.complex

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import net.leloubil.fossilwidgets.widgetsapi.makeProvider
import net.leloubil.fossilwidgets.widgets.WidgetContentProvider
import net.leloubil.fossilwidgets.widgetsapi.TextWidget
import net.leloubil.fossilwidgets.widgetsapi.widgetDataProvider
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
fun ScrollableTextWidgetContentProvider(
    windowSize: Int = 20,
    charsPerScroll: Int = 8,
    scrollDuration: Duration = 1.seconds,
    scrollWaitAtStart: Duration = 4.seconds,
    scrollWaitAtEnd: Duration = 4.seconds,
    sourceProviderState: StateFlow<WidgetContentProvider>
): WidgetContentProvider = makeProvider parent@{
    // flow that gives
    val sourceFlow =
        sourceProviderState.flatMapLatest { it(coroutineScope, context) }.stateIn(coroutineScope)
    // either when source changes state or when a scroll duration elapsed
    // emit a new state consiting of the current state of the source offset by the current scroll position


    val topFlow = widgetDataProvider topFlow@{
        var topScroll = 0
        sourceFlow.collectLatest {
            topScroll = 0
            this@topFlow.emit(topScroll)
            while (currentCoroutineContext().isActive) {
                if (topScroll == 0) {
                    delay(scrollWaitAtStart)
                    if (sourceFlow.value.topText.length > windowSize) {
                        topScroll += charsPerScroll
                    }
                } else if (topScroll + windowSize >= sourceFlow.value.topText.length) {
                    delay(scrollWaitAtEnd)
                    topScroll = 0
                } else {
                    delay(scrollDuration)
                    topScroll += charsPerScroll
                }
                Log.d(
                    "ScrollableTextWidgetContentProvider",
                    "Emitting top scroll $topScroll"
                )
                this@topFlow.emit(topScroll)
            }
        }

    }
    val bottomFlow = widgetDataProvider { // bottom scroll

        var bottomScroll = 0

        sourceFlow.collectLatest {
            bottomScroll = 0
            emit(bottomScroll)
            while (currentCoroutineContext().isActive) {
                if (bottomScroll == 0) {
                    delay(scrollWaitAtStart)
                    if (sourceFlow.value.bottomText.length > windowSize) {
                        bottomScroll += charsPerScroll
                    }
                } else if (bottomScroll + windowSize >= sourceFlow.value.bottomText.length) {
                    delay(scrollWaitAtEnd)
                    bottomScroll = 0
                } else {
                    delay(scrollDuration)
                    bottomScroll += charsPerScroll
                }
                Log.d(
                    "ScrollableTextWidgetContentProvider",
                    "Emitting bottom scroll $bottomScroll"
                )
                emit(bottomScroll)
            }
        }

    }

    sourceFlow.mapState { source ->
        topFlow.mapState { topScroll ->
            bottomFlow.mapState { bottomScroll ->
                TextWidget(
                    source.topText.substring(
                        topScroll,
                        min(topScroll + windowSize, source.topText.length)
                    ),
                    source.bottomText.substring(
                        bottomScroll,
                        min(bottomScroll + windowSize, source.bottomText.length)
                    )
                )
            }
        }
    }


}
