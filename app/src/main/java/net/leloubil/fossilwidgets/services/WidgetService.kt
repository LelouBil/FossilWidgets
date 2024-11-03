package net.leloubil.fossilwidgets.services

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import net.leloubil.fossilwidgets.widgets.WidgetsManager

class WidgetService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        Log.i("FossilWidgets", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i("FossilWidgets", "Starting service onStart")
        lifecycleScope.launch {
            Toast.makeText(this@WidgetService, "Widget Service started", Toast.LENGTH_LONG).show()
            try {
                supervisorScope {
                    val watchers = WidgetsManager.widgets.map { widget ->
                        async { widget.watchChanges(this@launch, this@WidgetService) }
                    }
                    // wait for all watchers to finish
                    watchers.forEach { it.await() }
                }
            }
            catch(e: CancellationException) {
                Log.i("FossilWidgets", "Service cancelled")
            }
            catch (e: Exception) {
                Log.e("FossilWidgets", "Error in widget watchers", e)
            }
        }
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i("FossilWidgets", "Stopping service")
        Toast.makeText(this, "Widget Service stopped", Toast.LENGTH_LONG).show()
    }
}