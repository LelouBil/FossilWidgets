package net.leloubil.fossilwidgets.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.leloubil.fossilwidgets.services.WidgetService
import net.leloubil.fossilwidgets.widgetsapi.WatchFace
import net.leloubil.fossilwidgets.widgetsapi.WatchFaceProvider
import net.leloubil.fossilwidgets.widgetsapi.WidgetComposeState
import net.leloubil.fossilwidgets.widgetsapi.WidgetContentProvider
import net.leloubil.fossilwidgets.widgetsapi.makeWatchface

fun WidgetComposeState.EmptyWatchFace() = makeWatchface {
    MutableStateFlow( WatchFace(
        "empty",
        listOf()
    ))
}


object WidgetsManager {
    lateinit var widgetService: WatchWidgetService
    val watchface: WatchFaceProvider?
        get() = _watchface
    private var _watchface: WatchFaceProvider? = null


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
        this._watchface = watchface
        restartWidgetService(context)
    }
}