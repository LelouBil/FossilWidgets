package net.leloubil.fossilwidgets

import android.util.Log
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.periodUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import net.leloubil.fossilwidgets.api.CompositionContext
import net.leloubil.fossilwidgets.api.Scrollable
import net.leloubil.fossilwidgets.api.Text
import net.leloubil.fossilwidgets.api.WatchFace
import net.leloubil.fossilwidgets.api.makeReactive
import net.leloubil.fossilwidgets.api.makeWatchFace
import net.leloubil.fossilwidgets.inputs.TimedLatchInput
import net.leloubil.fossilwidgets.stateproviders.CalendarEvent
import net.leloubil.fossilwidgets.stateproviders.MediaState
import net.leloubil.fossilwidgets.stateproviders.getMediaStateFlow
import net.leloubil.fossilwidgets.stateproviders.nextCalendarEventFlow
import net.leloubil.fossilwidgets.stateproviders.timeEventFlow
import net.leloubil.fossilwidgets.widgets.WidgetContent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds


val temporaryEvent = TimedLatchInput("event", 10.seconds, false) { it.toBooleanStrict() }
val enableScrolling = TimedLatchInput("scrolling", 15.seconds, false) { it.toBooleanStrict() }
val showLocation = TimedLatchInput("location", 15.seconds, false) { it.toBooleanStrict() }


fun CompositionContext<WatchFace>.TopWidget() =
    makeReactive<WidgetContent> {
        val mediaState by useState { getMediaStateFlow(context, coroutineScope) }
        val temporaryEvent by useState { temporaryEvent.toState() }
        if (mediaState.isPlaying && !temporaryEvent) {
            MediaInfo(mediaState)
        } else {
            NextEvent()
        }
    }

fun formatTime(time: Instant) = time.toLocalDateTime(TimeZone.currentSystemDefault()).time.format(
    LocalTime.Format {
        hour(); char(':'); minute();
    }
)

fun formatTimeWithDifference(time: Instant, relativeTo: Instant): String {
    val startDiff = relativeTo.periodUntil(time,TimeZone.currentSystemDefault())
    Log.i("format","start diff: $startDiff")
    val nextIn = if(startDiff.hours == 0) {
        "${startDiff.minutes}m"
    } else{
        "${startDiff.hours}h"
    }
    return formatTime(time) + " ($nextIn)"
}

private fun CompositionContext<WidgetContent>.NextEvent() =
    makeReactive<WidgetContent> {
        val nextEvent by useState { nextCalendarEventFlow(context, coroutineScope).toState() }
        val enableScrolling by useState { enableScrolling.toState() }
        val showLocation by useState { showLocation.toState() }
        if (nextEvent == null) {
            Text(Time(), "")
        } else {
            val event = nextEvent!!

            Text(
                Scrollable(enabled = enableScrolling) { event.title.static() },
                if (showLocation) Scrollable(enabled = enableScrolling) {
                    (event.location ?: "n/a").static()
                }
                else formatEventBottom(event).static()
            )
        }
    }

private fun formatEventBottom(
    event: CalendarEvent
): String {
    val tomorrow =
        Clock.System.now().plus(DatePeriod(days = 1), TimeZone.currentSystemDefault())
    val inTwoDays =
        Clock.System.now().plus(DatePeriod(days = 2), TimeZone.currentSystemDefault())
    val now = Clock.System.now()
    return if (event.start > now) {
        // before
        // if event is more than a day away, show the date
        if (event.start > tomorrow) {
            if (event.start > inTwoDays) {
                val dayDifference =
                    now.periodUntil(event.start, TimeZone.currentSystemDefault()).days
                event.start.toLocalDateTime(TimeZone.currentSystemDefault()).format(
                    LocalDateTime.Format {
                        dayOfMonth(); char('/'); monthNumber(); char('/');
                    }
                ) + " (${dayDifference}j)"
            } else {
                "Demain"
            }
        } else {
           formatTimeWithDifference(event.start,now)
        }
    } else {
        // during
        "-> " + formatTimeWithDifference(event.end,now)
    }
}

private fun CompositionContext<WidgetContent>.MediaInfo(
    mediaState: MediaState
) = makeReactive<WidgetContent> {
    val enableScrolling by useState { enableScrolling.toState() }
    Text(
        Scrollable(enabled = enableScrolling) {
            "${mediaState.metadata?.title}".static()
        },
        Scrollable(enabled = enableScrolling) {
            "${mediaState.metadata?.artist}".static()
        }
    )
}


fun Time() = // format time
    makeReactive<String> {
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

fun CompositionContext<WatchFace>.BottomWidget() = makeReactive<WidgetContent> {
    val scrolling by useState { enableScrolling.toState() }
    val forceEvent by useState { temporaryEvent.toState() }
    Text(
        if (scrolling) {
            "S"
        } else {
            " "
        }.static(),
        if (forceEvent) {
            "E"
        } else {
            " "
        }.static()
    )
}

fun BaseWatchFace() = makeReactive<WatchFace> {
    makeWatchFace(
        "Main",
        TopWidget(),
        BottomWidget()
    )
}


