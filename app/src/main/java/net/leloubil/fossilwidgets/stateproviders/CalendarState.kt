package net.leloubil.fossilwidgets.stateproviders

import android.database.Cursor
import android.provider.CalendarContract
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant
import net.leloubil.fossilwidgets.api.CompositionContext
import net.leloubil.fossilwidgets.widgets.WidgetContent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

//
data class CalendarEvent(val title: String, val start: Instant, val end: Instant)

suspend fun CompositionContext.nextCalendarEventFlow(
    updateTime: Duration = 10.minutes
) = channelFlow {
    while (currentCoroutineContext().isActive) {
        // get next calendar event that is not all day
        val cursor: Cursor? = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            ),
            "${CalendarContract.Events.ALL_DAY} = 0 AND (${CalendarContract.Events.DTSTART} >= ${System.currentTimeMillis()} OR ${CalendarContract.Events.DTEND} >= ${System.currentTimeMillis()})",
            null,
            "${CalendarContract.Events.DTSTART} ASC"
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val title: String =
                    cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                val start: String =
                    cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                val end: String =
                    cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                send(
                    CalendarEvent(
                        title,
                        Instant.fromEpochMilliseconds(start.toLong()),
                        Instant.fromEpochMilliseconds(end.toLong())
                    )
                )
            }
            cursor.close()
        }
        delay(updateTime)
    }
}.stateIn(coroutineScope)

