package net.leloubil.fossilwidgets.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import net.leloubil.fossilwidgets.api.WatchFaceProvider
import net.leloubil.fossilwidgets.services.WidgetService


object WidgetsManager {
    lateinit var widgetService: WatchWidgetService
    lateinit var watchFaceProvider: WatchFaceProvider
        private set


    private fun stopWidgetService(context: Context) {
        Log.i("WidgetsManager", "Stopping widget service")
        context.stopService(Intent(context, WidgetService::class.java))
    }

    private fun startWidgetService(context: Context) {
        Log.i("WidgetsManager", "Starting widget service")
        context.startService(Intent(context, WidgetService::class.java))
    }

    private fun restartWidgetService(context: Context) {
        stopWidgetService(context)
        startWidgetService(context)
    }

    fun setWatchface(watchface: WatchFaceProvider, context: Context) {
        this.watchFaceProvider = watchface
        restartWidgetService(context)
    }
}