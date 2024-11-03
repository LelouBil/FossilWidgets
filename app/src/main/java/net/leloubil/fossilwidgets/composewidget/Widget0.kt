package net.leloubil.fossilwidgets.composewidget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.leloubil.fossilwidgets.stateproviders.getMediaStateFlow
import net.leloubil.fossilwidgets.stateproviders.timeEventFlow
import net.leloubil.fossilwidgets.widgetsapi.TextWidget
import net.leloubil.fossilwidgets.widgetsapi.WidgetContentProvider
import net.leloubil.fossilwidgets.widgetsapi.makeProvider
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


fun Widget0(): WidgetContentProvider = makeProvider {
    getMediaStateFlow().mapState { mediaState ->
        if (mediaState.isPlaying) {
            if (mediaState.metadata != null) {
                TextWidget(mediaState.metadata.title, mediaState.metadata.artist)
            } else {
                TextWidget("Unknown", "Unknown")
            }
        } else {
            DifferingTopBottom(
                timeEventFlow(ChronoUnit.MINUTES).mapStateC { time ->
                    DateTimeFormatter.ofPattern("HH:mm")
                        .format(time.atZone(ZoneId.systemDefault()))
                },
                MutableStateFlow("")
            )

        }
    }
}

fun WidgetTest(): WidgetContentProvider = makeProvider {
    getMediaStateFlow().mapState { mediaState ->
        TextWidget("Test", "Test")
    }
}


fun DifferingTopBottom(
    topAction: StateFlow<String>,
    bottomAction: StateFlow<String>
): WidgetContentProvider = makeProvider {
    topAction.mapState { top ->
        bottomAction.mapState { bottom ->
            TextWidget(top, bottom)
        }
    }
}

