package net.leloubil.fossilwidgets.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.leloubil.fossilwidgets.inputs.Input
import net.leloubil.fossilwidgets.widgets.WidgetContent

class DependencyTracker() {
    val dependentFlows = mutableMapOf<String, StateFlow<*>>()
}

class StateFlowDelegate<T>(
    private val tracker: DependencyTracker,
    private val stateFlowGenerator: () -> StateFlow<T>
) {
    operator fun getValue(thisRef: Any?, property: Any?): T {
        Log.i("StateFlowDelegate", "Getting value for $thisRef, $property")
        if (!tracker.dependentFlows.containsKey(property.toString())) {
            val stateFlow = stateFlowGenerator()
            tracker.dependentFlows[property.toString()] = stateFlow
            return stateFlow.value
        } else {
            Log.i("StateFlowDelegate", "Already have stateflow for $property")
            @Suppress("UNCHECKED_CAST")
            val stateFlow = tracker.dependentFlows[property.toString()] as StateFlow<T>
            return stateFlow.value
        }
    }
}


abstract class CompositionContext(val context: Context, val coroutineScope: CoroutineScope) {
    private val tracker = DependencyTracker()

    //    fun <T> Flow<T>.asState(): StateFlowDelegate<T> {
//        if (this is StateFlow) {
//            return StateFlowDelegate(tracker, this)
//        }
//        return StateFlowDelegate(tracker, runBlocking {
//            stateIn(coroutineScope)
//        }
//        )
//    }
    fun <T> Flow<T>.toState() = runBlocking { stateIn(coroutineScope) }

    //    fun <T> StateFlow<T>.asState() = StateFlowDelegate(tracker, this)
    fun <T> useState(action: () -> StateFlow<T>): StateFlowDelegate<T> =
        StateFlowDelegate(tracker, action)

    fun <T> Input<T>.toState() = this.getInput(coroutineScope).toState()

    companion object {
        fun <TContext : CompositionContext, TOut> makeComposer(
            contextConstructor: (context: Context, coroutineScope: CoroutineScope) -> TContext
        )
                : ((action: TContext.() -> (TContext.() -> Flow<TOut>)) -> TContext.() -> Flow<TOut>) =
            { action ->
                a@{
                    Log.i(
                        "makeComposer",
                        "Creating channel flow, scope status: ${coroutineScope.isActive}, scope: $coroutineScope"
                    )
                    return@a channelFlow {
                        try {
                            // first composition
                            Log.i(
                                "makeComposer",
                                "Inside channel flow, scope status: ${coroutineScope.isActive}, scope: $coroutineScope"
                            )
                            Log.i(
                                "makeComposer",
                                "Creating first composition, new subscope ${this@channelFlow}"
                            )
                            val receiver = contextConstructor(context, this@channelFlow)
                            val dependencies = mutableSetOf<StateFlow<*>>()
                            assert(tracker.dependentFlows.isEmpty())
                            dependencies.clear()
                            val firstFlowProvider = with(receiver) {
                                action()
                            }
                            dependencies.addAll(receiver.tracker.dependentFlows.values)
                            tracker.dependentFlows.putAll(receiver.tracker.dependentFlows)
                            receiver.tracker.dependentFlows.clear()
                            Log.i("makeComposer", "Created first flow provider")
                            val flow = with(receiver) { // no dependentFlows here
                                firstFlowProvider()
                            }
                            Log.i("makeComposer", "Created first flow")
                            Log.i("makeComposer", "Added dependencies : $dependencies")
                            Log.i(
                                "makeComposer",
                                "Launching first composition, scope status: ${receiver.coroutineScope.isActive}"
                            )
                            var oldComposition = receiver.coroutineScope.launch {
                                try {
                                    Log.i("makeComposer", "Starting first composition")
                                    flow.collectLatest {
                                        Log.i("makeComposer", "Received first value: $it")
                                        Log.i("makeComposer", "Sending upwards first value $it")
                                        this@channelFlow.send(it)
                                        Log.i("makeComposer", "Sent upwards first value $it")
                                    }
                                    Log.i("makeComposer", "Exited first composition")
//                                } catch (e: Exception) {
//                                    Log.e("makeComposer", "Error in first composition", e)
//                                } catch (e: CancellationException) {
//                                    throw e
                                } finally {
                                    Log.i("makeComposer", "Finally block cancelled")
                                }
                            }
                            Log.i(
                                "makeComposer",
                                "Launched first composition, job: $oldComposition"
                            )
                            receiver.coroutineScope.launch {
                                try {
                                    Log.i("makeComposer", "Starting composition loop")
                                    while (currentCoroutineContext().isActive) {
                                        Log.i("makeComposer", "Waiting for dependencies")
                                        if (dependencies.isNotEmpty()) {
                                            val first =
                                                dependencies.map { s ->
                                                    s.drop(1) // stateflow replays the last value on collection, so we drop it
                                                        .map { sa -> sa.also { Log.i("StateObserver","Dependency changed: $sa") } }
                                                }.merge().first()
                                            Log.i(
                                                "makeComposer",
                                                "First dependency updated: $first"
                                            )
                                        } else
                                            awaitCancellation()
                                        Log.i("makeComposer", "Dependencies updated")
                                        oldComposition.cancel()

                                        Log.i("makeComposer", "Creating new composition")
                                        oldComposition =
                                            receiver.coroutineScope.launch newComposition@{
                                                val newReceiver =
                                                    contextConstructor(context, this@newComposition)
                                                Log.i(
                                                    "makeComposer",
                                                    "Created new receiver, new subscope ${this@newComposition}"
                                                )
                                                Log.i("makeComposer", "Creating new flow")

                                                dependencies.clear()
                                                newReceiver.tracker.dependentFlows.putAll(tracker.dependentFlows)
                                                val flowProvider = with(newReceiver) {
                                                    action()
                                                }
                                                dependencies.addAll(newReceiver.tracker.dependentFlows.values)
                                                tracker.dependentFlows.putAll(newReceiver.tracker.dependentFlows)
                                                newReceiver.tracker.dependentFlows.clear()
                                                var lastVal: TOut? = null
                                                try {
                                                    val newFlow = with(newReceiver) { // no dependentFlows here
                                                        flowProvider()
                                                    }
                                                    newFlow.collectLatest {
                                                        Log.i(
                                                            "makeComposer",
                                                            "Received new value: $it"
                                                        )
                                                        Log.i(
                                                            "makeComposer",
                                                            "Sending upwards new value $it"
                                                        )
                                                        this@channelFlow.send(it)
                                                        lastVal = it
                                                        Log.i(
                                                            "makeComposer",
                                                            "Sent upwards new value $it"
                                                        )
                                                    }
//                                                } catch (e: Exception) {
//                                                    Log.e("makeComposer", "Error in new flow", e)
//                                                } catch (e: CancellationException) {
//                                                    throw e
//                                                }
                                                } finally {
                                                    Log.i(
                                                        "makeComposer",
                                                        "Exited new flow, last value: $lastVal"
                                                    )
                                                }
                                            }
                                        Log.i(
                                            "makeComposer",
                                            "Launched new composition, job: $oldComposition"
                                        )
                                    }
                                    Log.i("makeComposer", "Exited composition loop")
//                                } catch (e: Exception) {
//                                    Log.e("makeComposer", "Error in composition loop", e)
//                                } catch (e: CancellationException) {
//                                    throw e
                                } finally {
                                    Log.i("makeComposer", "Finally block cancelled deps")
                                }
                            }
                            Log.i(
                                "makeComposer",
                                "Launched composition loop, job: $oldComposition, scope status: ${receiver.coroutineScope.isActive}, scope: ${receiver.coroutineScope}"
                            )

                            awaitCancellation()
                            Log.i("makeComposer", "Closed channel")
//                        } catch (e: Exception) {
//                            Log.e("makeComposer", "Error in channel flow", e)
//                        } catch (e: CancellationException) {
//                            throw e
                        } finally {
                            Log.i("makeComposer", "Finally block cancelled channel")
                        }
                    }

                }

            }
    }
}


// widgets
class WidgetsApiContext(
    context: Context,
    coroutineScope: CoroutineScope
) : CompositionContext(context, coroutineScope) {

    fun StringProvider.toState() = this@toState(StringContext(context, coroutineScope)).toState()

    fun String.static(): StringProvider = {
        MutableStateFlow(this@static).also {
            Log.i("WidgetsApiContext", "Created static string flow based on ${this@static}")
        }
    }

    companion object {
        val makeWidget2 by lazy {
            makeComposer<WidgetsApiContext, WidgetContent>(::WidgetsApiContext)
        }
    }
}


typealias WidgetProvider = WidgetsApiContext.() -> Flow<WidgetContent>

fun makeWidget(
    action: WidgetsApiContext.() -> WidgetProvider
): WidgetProvider =
    WidgetsApiContext.makeWidget2(action)


class StringContext(
    context: Context,
    coroutineScope: CoroutineScope
) : CompositionContext(context, coroutineScope) {
    companion object {
        val makeString by lazy {
            makeComposer<StringContext, String>(::StringContext)
        }
    }

    fun String.static(): StringProvider = { MutableStateFlow(this@static) }
    fun StringProvider.toState() = this@toState(StringContext(context, coroutineScope)).toState()
}

fun StaticString(string: String): StringProvider = { MutableStateFlow(string) }

typealias StringProvider = StringContext.() -> Flow<String>
typealias StringProviderCreator = StringContext.() -> StringProvider

fun makeStringProvider(
    action: StringContext.() -> StringProvider
): StringProvider =
    StringContext.makeString(action)


data class WatchFace(
    val name: String,
    val widgets: List<WidgetProvider>
)

// watchfaces
class WatchFaceContext(
    context: Context,
    coroutineScope: CoroutineScope
) : CompositionContext(context, coroutineScope) {
    companion object {
        val makeWatchface2 by lazy {
            makeComposer<WatchFaceContext, WatchFace>(
                ::WatchFaceContext
            )
        }
    }

    fun makeWatchFace(string: String, vararg widgets: WidgetProvider): WatchFaceProvider =
        { MutableStateFlow(WatchFace(string, widgets.toList())) }
}

typealias WatchFaceProvider = WatchFaceContext.() -> Flow<WatchFace>

fun makeWatchFaceProvider(
    action: WatchFaceContext.() -> WatchFaceProvider
): WatchFaceProvider =
    WatchFaceContext.makeWatchface2(action)


fun <T, U> StateFlow<T>.stateMap(
    coroutineScope: CoroutineScope,
    action: (T) -> U,
    sharingStarted: SharingStarted = SharingStarted.Lazily
): StateFlow<U> {
    return channelFlow {
        this@stateMap.collectLatest {
            send(action(it))
        }
    }.stateIn(coroutineScope, sharingStarted, action(value))
}

fun <T, U> StateFlow<T>.stateFlatMap(
    coroutineScope: CoroutineScope,
    action: (T) -> StateFlow<U>,
    sharingStarted: SharingStarted = SharingStarted.Lazily
): StateFlow<U> {
    return channelFlow {
        this@stateFlatMap.collectLatest {
            action(it).collectLatest { sub ->
                send(sub)
            }
        }
    }.stateIn(coroutineScope, sharingStarted, action(value).value)
}

