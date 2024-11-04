package net.leloubil.fossilwidgets.services

import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import net.leloubil.fossilwidgets.inputs.MenuInputReceiver
import net.leloubil.fossilwidgets.widgets.WidgetContent
import net.leloubil.fossilwidgets.widgets.WidgetsManager
import net.leloubil.fossilwidgets.widgetsapi.WidgetComposeState

class WidgetService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        Log.i("FossilWidgets", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        ContextCompat.registerReceiver(this,MenuInputReceiver, IntentFilter("nodomain.freeyourgadget.gadgetbridge.Q_COMMUTE_MENU"),
            ContextCompat.RECEIVER_EXPORTED)
        Log.i("FossilWidgets", "Starting service onStart")
        lifecycleScope.launch {
            Toast.makeText(this@WidgetService, "Widget Service started", Toast.LENGTH_LONG).show()
            try {
                WidgetsManager.watchface?.let {
                    it(
                        WidgetComposeState(
                            this@launch,
                            this@WidgetService
                        )
                    )
                }
                    ?.collectLatest { watchFace ->
                        Log.i("FossilWidgets", "New watchface: $watchFace")
                        WidgetsManager.widgetService.setWatchFace(this@WidgetService, watchFace.name)
                        supervisorScope {
                            watchFace.widgets.forEachIndexed { index, widgetContentProvider ->
                                launch {
                                    var oldContent: WidgetContent? = null
                                    widgetContentProvider.collectLatest {
                                        coroutineScope {
                                            if (oldContent != it) {
                                                oldContent = it
                                                WidgetsManager.widgetService.setWidget(
                                                    this@WidgetService,
                                                    index,
                                                    it.topText,
                                                    it.bottomText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            } catch (e: CancellationException) {
                Log.i("FossilWidgets", "Service cancelled")
            } catch (e: Exception) {
                Log.e("FossilWidgets", "Error in widget watchers", e)
            }
        }
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(MenuInputReceiver)
        Log.i("FossilWidgets", "Stopping service")
        Toast.makeText(this, "Widget Service stopped", Toast.LENGTH_LONG).show()
    }
}