package net.leloubil.fossilwidgets.widgetsapi

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import net.leloubil.fossilwidgets.widgets.WidgetContent


typealias WidgetContentProvider = suspend (CoroutineScope, Context) -> StateFlow<WidgetContent>

fun makeProvider(
    action: suspend WidgetComposeStateInWidget.() -> StateFlow<WidgetContent>
): WidgetContentProvider =
    { coroutineScope, context ->
        channelFlow {
            with(WidgetComposeStateInWidget(this@channelFlow, context)) {
                action().collectLatest {
                    send(it)
                }
            }
        }.stateIn(coroutineScope)
    }

class WidgetDataProviderState<T>(
    private val producerScope: ProducerScope<T>,
    val coroutineScope: CoroutineScope,
    val context: Context
) {
    suspend fun emit(content: T) {
        producerScope.send(content)
    }
}


suspend fun <T> WidgetComposeStateInWidget.widgetDataProvider(
    action: suspend WidgetDataProviderState<T>.() -> Unit
): StateFlow<T> =

    channelFlow {
        val receiver = WidgetDataProviderState<T>(this@channelFlow, coroutineScope, context)
        receiver.action()
    }.stateIn(coroutineScope)


class WidgetComposeStateInWidgetInMapState(coroutineScope: CoroutineScope, context: Context): WidgetComposeStateInWidget(coroutineScope, context) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T, R> StateFlow<T>.mapState(
        action: suspend WidgetComposeStateInWidgetInMapState.(T) -> StateFlow<R>
    ): StateFlow<R> =
        this@mapState.flatMapLatest { data ->
            action(data)
        }.stateIn(coroutineScope)


    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T, R> StateFlow<T>.mapStateC(
        action: suspend WidgetComposeStateInWidgetInMapState.(T) -> R
    ): StateFlow<R> =
        this@mapStateC.mapLatest { data ->
            action(data)
        }.stateIn(coroutineScope)
}

open class WidgetComposeStateInWidget(
    val coroutineScope: CoroutineScope,
    val context: Context
) {

    @OptIn(ExperimentalCoroutinesApi::class)
   suspend fun <T> StateFlow<T>.mapState(
        action: suspend WidgetComposeStateInWidgetInMapState.(T) -> WidgetContentProvider
    ): StateFlow<WidgetContent> =
       this@mapState.flatMapLatest { data ->
           WidgetComposeStateInWidgetInMapState(coroutineScope, context).action(data)(coroutineScope, context)
       }.stateIn(coroutineScope)


}