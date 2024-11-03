package net.leloubil.fossilwidgets.stateproviders

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.leloubil.fossilwidgets.widgetsapi.WidgetComposeStateInWidget
import net.leloubil.fossilwidgets.widgetsapi.widgetDataProvider
import java.time.Instant
import java.time.temporal.ChronoUnit


suspend fun WidgetComposeStateInWidget.timeEventFlow(step: ChronoUnit) = widgetDataProvider<Instant> {
    emit(Instant.now())
    val stepAsMillis = step.duration.toMillis()
    delay(step.duration.toMillis() - Instant.now().toEpochMilli() % stepAsMillis)
    while (currentCoroutineContext().isActive) {
        // delay until next multiple of step
        emit(Instant.now())
        delay(stepAsMillis)
    }
}
