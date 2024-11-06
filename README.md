# Fossil Hybrid Smartwatch widgets using Gadgetbridge

This is an android application that uses Gadgetbridge's Intents API to change the contents of custom widgets on a Fossil Hybrid Smartwatch. It aims to be easy to use to program dynamic widgets, instead of using apps like Tasker for this (Tasker works well, I was using it. But for complex widgets it is really cumbersome to set up)

Right now the widgets are "hardcoded" inside of the app (Widgets.kt), but the end goal would be dynamic code loaded from other APKs, and this app would be responsible for calling them.

I was using Tasker to do custom widgets, but I wanted advanced stuff like Scrolling text and switching values based on context. This quickly became unmanageable inside of tasker so I thought of having a declarative React/Compose-like api to build custom watchfaces and widgets.

Currently, it's working fine.I used flow/coroutine features as much as possible to make the application easy on battery life, since it's running in the background 24/7. There are no busy loops checking changed values, everything is reactive and flows are waiting for the corresponding android event handlers to re render.
State is currently preserved when re-rendering comes from the bottom of the component stack, but not when it comes from the top. 

Try to guess what the following code does :
```kotlin
val testLatch = TimedLatchInput("Test", 10.seconds, false) { it.toBooleanStrict() }
fun WatchFaceContext.SomeWidget() = makeWidget {
    val mediaState by useState { getMediaStateFlow(context, coroutineScope) }
    val latchInput by useState { testLatch.toState() }
    if (mediaState.isPlaying) {
        Text(
            NextEvent(),
            PlayState(mediaState, latchInput)
        )
    } else {
        NotPlaying()
    }
}

private fun WatchFaceContext.NextEvent() = makeStringProvider {
    val nextEvent by useState { nextCalendarEventFlow(context, coroutineScope).toState() }
    (nextEvent?.title ?: "No upcoming event").static()
}

private fun WidgetsApiContext.PlayState(mediaState: MediaState, scroll: Boolean) =
    Scrollable(windowSize = 8, enabled = scroll) {
        "Playing ${mediaState.metadata?.title}".static()
    }


fun WidgetsApiContext.NotPlaying() = makeWidget {
    Text("Hello", Time())
}

fun Time() = makeStringProvider {
    val currentTime by useState { timeEventFlow(DateTimeUnit.SECOND).toState() }
    // format time
    currentTime
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .time
        .format(
            LocalTime.Format {
                hour(); char(':'); minute(); char(':'); second()
            }
        ).static()
}

fun BaseWatchFace() = makeWatchFaceProvider {
    makeWatchFace(
        "Main",
        SomeWidget()
    )
}
```

Here are some step by step explanations of the code above:

# Reactivity Basics

The API for building WatchFaces and Widgets is inspired by React and Jetpack Compose. It is a declarative way of building UIs, where the UI is a function of the state. The state is reactive, and the UI re-renders when the state changes.
It is built on top of the Flow API, any Flow can be used as state by wrapping it in a `useState` call. Using the value produced by useState will add it as a "dependency" to the widget, and the widget will re-render when the flow emits a new value.

The API still has some limitations, like re-renders not working properly if you nest makeWidget calls, but you can always do separate functions and it will work fine.
The actual Flow objects are preserved only if the re-render comes from a state change. If the parent widget re-renders, the state will be re-created, and the Flows will be re-subscribed using the callables provided in the `useState` call.

# WatchFaces
At the root of the component tree, there is a WatchFaceProvider. It is responsible for selecting the WatchFace to switch to based on it's state. It has a name and a list (vararg) of widgets

In the example above it has no state, and so choses the WatchFace "Main" with the widget "SomeWidget" at the start of the application and never changes it.

Side note: WatchFaces are identified by their name, It would be to expensive to re-render if the same WatchFace is selected again, so it is ignored, even if it has different widgets.

The way to make the Widgets different for the same WatchFace is to use state inside of the Widgets themselves.

# Widgets
Widgets are responsible for producing the data that will actually be displayed on the watch. They re-render if any of their state (useState) changes. And if they re-rendered because of a state change, they will keep the exact same instances of their state objects.

In the example above, the widget "SomeWidget" has two states: mediaState and latchInput. The mediaState listens to the current media state of the phone, and the latchInput is a TimedLatchInput is a latch input (more on it below)

# Input
This is a way to get data from the watch. It builds on the Custom Menu functionality.
Currently, it listens to the custom menu intent and splits the sent data in 2 by ':'

The first part is interpreted as the name of the input, and the second part is the value. The value is then passed to the callable provided in the constructor of the Input object.

Basically, since the whole api is built on Flows, Inputs are a way to get a Flow from a watch event.

Right now, there are 2 types of Inputs: BooleanInput and TimedLatchInput

## BooleanInput
This is a simple input that listens to the custom menu intent and uses the provided callable to convert the string to a boolean.

Using it as state is as simple as every other state, and the widget will re-render when the value changes.

## TimedLatchInput

This one is very useful. It is a boolean input that will return true for a certain amount of time after it receives a true value, and then revert to false.

It is useful for things like enabling scrolling text for a certain amount of time after a button press. (This is what the latchInput in the example above is used for) 
