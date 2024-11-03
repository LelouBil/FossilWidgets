package net.leloubil.fossilwidgets.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class Widget(private val widgetId: Int, private val widgetService: WatchWidgetService) {

    private var oldContent = WidgetContent("", "")


    private var _contentProvider: MutableStateFlow<WidgetContentProvider?> = MutableStateFlow(null)
    var contentProvider: WidgetContentProvider?
        get() = _contentProvider.value
        set(value) {
            Log.i("Widget", "Setting content provider for widget $widgetId to $value")
            _contentProvider.value = value
        }


    suspend fun watchChanges(coroutineScope: CoroutineScope, context: Context) =
        with(coroutineScope) {
            Log.i("Widget", "Starting WatchChanges for widget $widgetId")
            var oldProviderJob: Job? = null
            Log.i("Widget", "Watching changes for widget $widgetId")
            _contentProvider.collectLatest { newProvider ->
                oldProviderJob?.cancel()
                Log.i("Widget", "New provider for widget $widgetId: $newProvider")
                if (newProvider != null) {
                    Log.i("Widget", "Non null provider for widget $widgetId")
                    oldProviderJob = launch {
                        Log.i("Widget", "Starting update for widget $widgetId")
                        newProvider(this, context)
                            .collect { content ->
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
                } else {
                    Log.i("Widget", "Null provider for widget $widgetId")
                    oldContent = WidgetContent("", "")
                    widgetService.setWidget(
                        context,
                        widgetId,
                        "",
                        ""
                    )
                }

            }
        }
}

