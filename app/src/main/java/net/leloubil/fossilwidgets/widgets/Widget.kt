package net.leloubil.fossilwidgets.widgets

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.collectLatest
import net.leloubil.fossilwidgets.widgetsapi.WidgetContentProvider

class Widget(
    private val widgetId: Int,
    private val contentProvider: WidgetContentProvider,
    private val widgetService: WatchWidgetService
) {

    private var oldContent = WidgetContent("", "")


    suspend fun watchChanges(context: Context) {
        Log.i("Widget", "Starting WatchChanges for widget $widgetId")
        return contentProvider.collectLatest { content ->
            Log.i("Widget", "New content for widget $widgetId: $content")
            if (content != oldContent) {
                Log.i("Widget", "Content changed for widget $widgetId")
                oldContent = content
                widgetService.setWidget(
                    context,
                    widgetId,
                    content.topText,
                    content.bottomText
                )
            }
        }
    }

}

