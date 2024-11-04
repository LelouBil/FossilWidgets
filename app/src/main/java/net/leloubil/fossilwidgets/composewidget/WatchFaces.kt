package net.leloubil.fossilwidgets.composewidget

import kotlinx.coroutines.flow.distinctUntilChangedBy
import net.leloubil.fossilwidgets.inputs.TimedLatchInput
import net.leloubil.fossilwidgets.stateproviders.getMediaStateFlow
import net.leloubil.fossilwidgets.stateproviders.timeEventFlow
import net.leloubil.fossilwidgets.widgetsapi.WatchFace
import net.leloubil.fossilwidgets.widgetsapi.WidgetComposeState
import net.leloubil.fossilwidgets.widgetsapi.WidgetContentProvider
import net.leloubil.fossilwidgets.widgetsapi.makeProvider
import net.leloubil.fossilwidgets.widgetsapi.makeWatchface
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

val testLatch = TimedLatchInput("Test", 2.seconds, false) { it.toBoolean() }

fun WidgetComposeState.BaseWatchFace() = makeWatchface {
    getMediaStateFlow().distinctUntilChangedBy { it.isPlaying }.useState { mediaState ->
        if (mediaState.isPlaying) {
            WatchFace(
                "Music",
                listOf(
                    MusicWidget()
                )
            )
        } else {
            WatchFace(
                "Main",
                listOf(
                    TimeWidget()
                )
            )
        }
    }
}

fun WidgetComposeState.MusicWidget(): WidgetContentProvider = makeProvider {
    getMediaStateFlow().useStateW { mediaState ->
        testLatch(this).useStateW { latchState ->
            if (latchState) {
                TextWidget("Latched", "")
            } else if (mediaState.isPlaying) {
                DifferingTopBottom(
                    if (mediaState.metadata != null) {
                        Scrollable(
                            LineText(mediaState.metadata.title)
                        )
                    } else {
                        LineText("Playing media")
                    },
                    Scrollable(
                        LineText(
                            mediaState.metadata?.artist ?: "Unknown artist"
                        )
                    )
                )
            } else {
                TextWidget("Not playing", "")
            }
        }
    }
}

fun WidgetComposeState.TimeWidget(): WidgetContentProvider = makeProvider {
    timeEventFlow(ChronoUnit.MINUTES).useStateW {
        TextWidget(
            DateTimeFormatter.ofPattern("HH:mm")
                .format(it.atZone(ZoneId.systemDefault())), ""
        )
    }
}
