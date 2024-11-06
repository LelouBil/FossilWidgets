package net.leloubil.fossilwidgets.services

import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.leloubil.fossilwidgets.api.CompositionContext
import net.leloubil.fossilwidgets.api.WatchFace
import net.leloubil.fossilwidgets.inputs.MenuInputReceiver
import net.leloubil.fossilwidgets.widgets.WidgetContent
import net.leloubil.fossilwidgets.widgets.WidgetsManager

class WidgetService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        Log.i("FossilWidgets", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        ContextCompat.registerReceiver(
            this,
            MenuInputReceiver,
            IntentFilter("nodomain.freeyourgadget.gadgetbridge.Q_COMMUTE_MENU"),
            ContextCompat.RECEIVER_EXPORTED
        )
        Log.i("FossilWidgets", "Starting service onStart")
        lifecycleScope.launch watchFace@{
            Toast.makeText(this@WidgetService, "Widget Service started", Toast.LENGTH_LONG).show()
            Log.i("FossilWidgets", "Starting service, lifecycle: ${this@watchFace}")
            try {
                WidgetsManager.watchFaceProvider(
                    CompositionContext<WatchFace>(
                        this@WidgetService,
                        this@watchFace
                    )
                )
                    .distinctUntilChangedBy { it.name } // don't reset widgets if watchface didn't change
                    .collectLatest { watchFace ->
                        WidgetsManager.widgetService.setWatchFace(
                            this@WidgetService,
                            watchFace.name
                        )
//                        supervisorScope { // if a widget crashes, don't crash the whole service
                            Log.i(
                                "FossilWidgets",
                                "Starting widgets, supervisor status: ${this.isActive}, supervisor: ${this}"
                            )
                            watchFace.widgets.forEachIndexed { index, widgetContentProvider ->
                                launch widget@{
                                    Log.i(
                                        "FossilWidgets",
                                        "Starting widget $index, scope status: ${this@widget.isActive}, scope: ${this@widget}"
                                    )
                                    var oldContent: WidgetContent? = null
                                    widgetContentProvider(
                                        CompositionContext<WidgetContent>(
                                            this@WidgetService,
                                            this@widget
                                        )
                                    )
                                        .collectLatest {
                                            Log.i(
                                                "FossilWidgets",
                                                "New content for widget $index: $it"
                                            )
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
                                    Log.i("FossilWidgets", "Widget $index finished")
                                }
                            }
                            Log.i("FossilWidgets", "All widgets started, waiting for cancellation")
                            awaitCancellation()
                        } //todo error handling (logging and restarting the widget)
                        Log.i("FossilWidgets", "All widgets finished")
//                    }
                Log.i("FossilWidgets", "Watchface finished")
                awaitCancellation()
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