# Fossil Hybrid Smartwatch widgets using Gadgetbridge

I was using Tasker to do custom widgets, but I wanted advanced stuff like Scrolling text and switching values based on context. This quickly became unmanageable inside of tasker so I thought of having a declarative React/Compose-like api to build custom watchfaces and widgets.

Currently, it's working fine. The end goal for the api is to drop all the nested lambas and have some kind of mechanism that tracks context and to be able to keep state even if the parent component re-renders (currently it's lost)

I used flow/coroutine features as much as possible to make the application easy on battery life, since it's running in the background 24/7. There are no busy loops checking changed values, everything is reactive and flows are waiting for the corresponding android event handlers to re render.
There may be a bit more re-render than needed right now for each value change.

```kotlin
// to get input from the custom menu. Syntax is <key>:<value>. Here the key is "test" and the value gets converted to a boolean via the function
// this input is a latch, it starts at the value false and when set to any other value it stays for 2 seconds and then goes back to false
val testLatch = TimedLatchInput("Test", 2.seconds, false) { it.toBoolean() }

// WatchFaces return a flow of WatchFace objects, which specify the name of the watchface to switch to and the list of widget content providers
// the distinctUntilChangedBy makes sure the same watchface doesn't get re switched to if the media metatata changed but not play state.
fun WidgetComposeState.BaseWatchFace() = makeWatchface {
    getMediaStateFlow().distinctUntilChangedBy { it.isPlaying }.useState { mediaState ->
        if (mediaState.isPlaying) {
            WatchFace(
                "Music",
                listOf(
                    MusicWidget()
                )
            )
        } else {
            WatchFace(
                "Main",
                listOf(
                    TimeWidget()
                )
            )
        }
    }
}

// the useStateW is kinda ugly, it's an early version of the api. It's basically a flatMap underneath (but which makes sure the old flow gets cancelled if the state changes)
fun WidgetComposeState.MusicWidget(): WidgetContentProvider = makeProvider {
    getMediaStateFlow().useStateW { mediaState ->
        testLatch(this).useStateW { latchState ->
            if (latchState) {
                TextWidget("Latched", "")
            } else if (mediaState.isPlaying) {
                DifferingTopBottom(
                    if (mediaState.metadata != null) {
                        Scrollable(
                            LineText(mediaState.metadata.title)
                        )
                    } else {
                        LineText("Playing media")
                    },
                    Scrollable(
                        LineText(
                            mediaState.metadata?.artist ?: "Unknown artist"
                        )
                    )
                )
            } else {
                TextWidget("Not playing", "")
            }
        }
    }
}

fun WidgetComposeState.TimeWidget(): WidgetContentProvider = makeProvider {
    timeEventFlow(ChronoUnit.MINUTES).useStateW {
        TextWidget(
            DateTimeFormatter.ofPattern("HH:mm")
                .format(it.atZone(ZoneId.systemDefault())), ""
        )
    }
}
```
