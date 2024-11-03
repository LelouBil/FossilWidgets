package net.leloubil.fossilwidgets.stateproviders

import android.database.Cursor
import android.provider.CalendarContract
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.leloubil.fossilwidgets.widgetsapi.widgetDataProvider
import net.leloubil.fossilwidgets.widgets.WidgetContent
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class CalendarEvent(val title: String, val start: Instant, val end: Instant)
fun nextCalendarEventFlow(
    updateTime: Duration = 10.minutes
) = widgetDataProvider {
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
                emit(WidgetContent(title, "Start: $start End: $end"))
            }
            cursor.close()
        }
        delay(updateTime)
    }
}

