package net.leloubil.fossilwidgets.stateproviders

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.until
import net.leloubil.fossilwidgets.api.CompositionContext
import net.leloubil.fossilwidgets.widgets.WidgetContent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

//
data class CalendarEvent(
    val title: String,
    val start: Instant,
    val end: Instant,
    val location: String? = null
)


private fun getNextEvent(context: Context): CalendarEvent? {
    val cursor: Cursor? = context.contentResolver.query(
        CalendarContract.Events.CONTENT_URI,
        arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        ),
        "${CalendarContract.Events.ALL_DAY} = 0 AND (${CalendarContract.Events.DTSTART} >= ${System.currentTimeMillis()} OR ${CalendarContract.Events.DTEND} >= ${System.currentTimeMillis()})",
        null,
        "${CalendarContract.Events.DTSTART} ASC"
    )

    return if (cursor?.moveToFirst() == true) {
        val title: String =
            cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
        val start: String =
            cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
        val end: String =
            cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
        val location: String? =
            cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION))
        CalendarEvent(
            title,
            Instant.fromEpochMilliseconds(start.toLong()),
            Instant.fromEpochMilliseconds(end.toLong()),
            location
        )
    } else {
        null
    }.also {
        cursor?.close()
    }
}

fun nextCalendarEventFlow(
    context: Context,
    coroutineScope: CoroutineScope,
    updateTime: Duration = 10.minutes
) = channelFlow {
    while (currentCoroutineContext().isActive) {
        // get next calendar event that is not all day
        val element = getNextEvent(context)
        send(element)
        val now = Clock.System.now()
        val time = {
            if (element == null) {
                updateTime
            } else {
                val untilStart = now.until(element.start, DateTimeUnit.MILLISECOND)
                val untilEnd = now.until(element.end, DateTimeUnit.MILLISECOND)
                if (now < element.start && untilStart < updateTime.inWholeMilliseconds)
                    untilStart
                else if (now > element.start && now < element.end && untilEnd < updateTime.inWholeMilliseconds) {
                    untilEnd
                }
            }
        }
        delay(updateTime)
    }
}.stateIn(coroutineScope, SharingStarted.Eagerly, getNextEvent(context))

