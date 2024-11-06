package net.leloubil.fossilwidgets.stateproviders

import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant


fun timeEventFlow(step: DateTimeUnit.TimeBased) = flow {
    val randInt = (0..100).random()
    Log.i("timeEventFlow", "randInt: $randInt")
    emit(Clock.System.now())
    val stepAsMillis = step.duration.inWholeMilliseconds
    try {
        delay(
            (step.duration.inWholeMilliseconds - Clock.System.now()
                .toEpochMilliseconds()) % stepAsMillis
        )
        while (currentCoroutineContext().isActive) {
            // delay until next multiple of step
            Log.i("timeEventFlow", "Emitting time $randInt")
            emit(Clock.System.now())
            delay(stepAsMillis)
        }
    } finally {
        Log.i("timeEventFlow", "Stopped emitting time $randInt")
    }
}
