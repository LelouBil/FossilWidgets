package net.leloubil.fossilwidgets.stateproviders

import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.leloubil.fossilwidgets.widgetsapi.WidgetComposeState
import net.leloubil.fossilwidgets.widgetsapi.widgetDataProvider
import java.time.Instant
import java.time.temporal.ChronoUnit


suspend fun WidgetComposeState.timeEventFlow(step: ChronoUnit) = widgetDataProvider<Instant> {
    val randInt = (0..100).random()
    Log.i("timeEventFlow", "randInt: $randInt")
    emit(Instant.now())
    val stepAsMillis = step.duration.toMillis()
    try{
    delay(step.duration.toMillis() - Instant.now().toEpochMilli() % stepAsMillis)
    while (currentCoroutineContext().isActive) {
        // delay until next multiple of step
        Log.i("timeEventFlow", "Emitting time $randInt")
        emit(Instant.now())
        delay(stepAsMillis)
    }}
    finally {
        Log.i("timeEventFlow", "Stopped emitting time $randInt")
    }
}
