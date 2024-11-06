package net.leloubil.fossilwidgets

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.leloubil.fossilwidgets.api.Scrollable
import net.leloubil.fossilwidgets.api.Text
import net.leloubil.fossilwidgets.api.WatchFaceContext
import net.leloubil.fossilwidgets.api.WidgetsApiContext
import net.leloubil.fossilwidgets.api.makeStringProvider
import net.leloubil.fossilwidgets.api.makeWatchFaceProvider
import net.leloubil.fossilwidgets.api.makeWidget
import net.leloubil.fossilwidgets.inputs.TimedLatchInput
import net.leloubil.fossilwidgets.stateproviders.MediaState
import net.leloubil.fossilwidgets.stateproviders.getMediaStateFlow
import net.leloubil.fossilwidgets.stateproviders.nextCalendarEventFlow
import net.leloubil.fossilwidgets.stateproviders.timeEventFlow
import kotlin.time.Duration.Companion.seconds


val testLatch = TimedLatchInput("Test", 10.seconds, false) { it.toBooleanStrict() }
fun WatchFaceContext.SomeWidget() = makeWidget {
    val mediaState by useState { getMediaStateFlow(context, coroutineScope) }
    val latchInput by useState { testLatch.toState() }
    if (mediaState.isPlaying) {
        Text(
            NextEvent(),
            PlayState(mediaState, latchInput)
        )
    } else {
        NotPlaying()
    }
}

private fun WatchFaceContext.NextEvent() = makeStringProvider {
    val nextEvent by useState { nextCalendarEventFlow(context, coroutineScope).toState() }
    (nextEvent?.title ?: "No upcoming event").static()
}

private fun WidgetsApiContext.PlayState(mediaState: MediaState, scroll: Boolean) =
    Scrollable(windowSize = 8, enabled = scroll) {
        "Playing ${mediaState.metadata?.title}".static()
    }


fun WidgetsApiContext.NotPlaying() = makeWidget {
    Text("Hello", Time())
}

fun Time() = makeStringProvider {
    val currentTime by useState { timeEventFlow(DateTimeUnit.SECOND).toState() }
    // format time
    currentTime
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .time
        .format(
            LocalTime.Format {
                hour(); char(':'); minute(); char(':'); second()
            }
        ).static()
}

fun BaseWatchFace() = makeWatchFaceProvider {
    makeWatchFace(
        "Main",
        SomeWidget()
    )
}


