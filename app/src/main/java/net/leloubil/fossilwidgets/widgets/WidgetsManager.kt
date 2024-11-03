package net.leloubil.fossilwidgets.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import net.leloubil.fossilwidgets.services.WidgetService

object WidgetsManager {
    val widgets: List<Widget>
        get() = _widgets
    private val _widgets = mutableListOf<Widget>()
    var widgetService: WatchWidgetService = RealWidgetService


    private fun stopWidgetService(context: Context) {
        Log.i("WidgetsManager", "Stopping widget service")
        context.stopService(Intent(context, WidgetService::class.java))
    }

    private fun startWidgetService(context: Context) {
        Log.i("WidgetsManager", "Starting widget service")
        context.startService(Intent(context, WidgetService::class.java))
    }

    fun restartWidgetService(context: Context) {
        stopWidgetService(context)
        startWidgetService(context)
    }

    fun setWidgetsCount(count: Int, context: Context) {
        if (count < 0) throw IllegalArgumentException("Count must be positive")
        _widgets.clear()
        for (i in 0..<count) {
            _widgets.add(Widget(i, widgetService).apply { contentProvider = null })
        }
        restartWidgetService(context)
    }
}