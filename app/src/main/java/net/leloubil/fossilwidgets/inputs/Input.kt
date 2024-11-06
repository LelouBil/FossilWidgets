package net.leloubil.fossilwidgets.inputs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

class BooleanInput(name: String, defaultValue: Boolean) : Input<Boolean>(name,defaultValue) {
    override suspend fun onReceive(data: Flow<String>): Flow<Boolean> =
        data.map { it.toBoolean() }
}

class TimedLatchInput<T>(
    name: String,
    private val resetDelay: Duration,
    private val defaultValue: T,
    private val conversionFunc: (String) -> T
) : Input<T>(name,defaultValue) {
    override suspend fun onReceive(data: Flow<String>): Flow<T> =
        channelFlow {
            var lastValue: T
            data.collectLatest {
                send(conversionFunc(it))
                Log.i("TimedLatchInput", "Received value AAA: $it")
                lastValue = conversionFunc(it)
                Log.i("TimedLatchInput", "Received value: $lastValue")
                send(lastValue)
                if (lastValue != defaultValue) {
                    Log.i("TimedLatchInput", "Resetting value in $resetDelay")
                    delay(resetDelay)
                    Log.i("TimedLatchInput", "Resetting value to $defaultValue")
                    send(defaultValue)
                    lastValue = defaultValue
                }
            }
            Log.i("TimedLatchInput", "Closing channel")
        }
}


abstract class Input<T>(private val name: String, private val defaultValue: T) {
    protected abstract suspend fun onReceive(data: Flow<String>): Flow<T>

    fun getInput(coroutineScope: CoroutineScope): StateFlow<T> = callbackFlow {
        val listener = Channel<String>()
        MenuInputReceiver.listeners[name] = listener
        Log.i("Input", "Listening for $name")
        onReceive(listener.receiveAsFlow()).collectLatest {
            Log.i("Input", "Sending value $it")
            send(it)
            Log.i("Input", "Sent value $it")
        }
        Log.i("Input", "waiting for close")
        awaitClose {
            Log.i("Input", "Closing input")
            MenuInputReceiver.listeners.remove(name)
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, defaultValue)

}

// android broadcast receiver


object MenuInputReceiver : BroadcastReceiver() {
    val listeners: HashMap<String, SendChannel<String>> = HashMap()
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("MenuInputReceiver", "Received intent $intent")
        intent.getStringExtra("EXTRA_ACTION")?.let { action ->
            Log.i("MenuInputReceiver", "Received action: $action")
            val (target, value) = action.split(":", limit = 2)
            Log.i("MenuInputReceiver", "Target: $target, value: $value")
            listeners[target]?.let { listener ->
                Log.i("MenuInputReceiver", "Sending value $value to $target")
                listener.trySend(value)
            }
        }
    }
}