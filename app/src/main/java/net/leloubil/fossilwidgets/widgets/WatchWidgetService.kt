package net.leloubil.fossilwidgets.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject

interface WatchWidgetService {
    fun setWidget(context: Context, widgetNum: Int, topText: String, bottomText: String)
    fun setWatchFace(context: Context, watchFace: String)
}

object RealWidgetService : WatchWidgetService {
    override fun setWidget(context: Context, widgetNum: Int, topText: String, bottomText: String) =
        setWidgetGadgetBridge(context, widgetNum, topText, bottomText)
    override fun setWatchFace(context: Context, watchFace: String) {
        setWatchFaceGadgetBridge(context, watchFace)
    }
}

class FakeWidgetService : WatchWidgetService {

    private val _watchFace = mutableStateOf("")
    public val watchFace: String
        get() = _watchFace.value

    private val _state = mutableStateListOf<WidgetContent>()

    public val state: List<WidgetContent>
        get() = _state

    override fun setWidget(context: Context, widgetNum: Int, topText: String, bottomText: String) {
        if (_state.size < widgetNum + 1) {
            _state.addAll(List((widgetNum + 1) - _state.size) { WidgetContent("", "") })
        }
        Log.d("FakeWidgetService", "Setting widget $widgetNum to $topText $bottomText")
        Log.d("FakeWidgetService", "Widget count: ${_state.size}")
        _state[widgetNum] = WidgetContent(topText, bottomText)
    }

    override fun setWatchFace(context: Context, watchFace: String) {
        _watchFace.value = watchFace
    }
}


const val SET_WIDGET_INTENT: String = "nodomain.freeyourgadget.gadgetbridge.Q_PUSH_CONFIG"
private fun setWidgetGadgetBridge(context: Context, widgetNum: Int, topText: String, bottomText: String) {
    Log.i("SetWidget", "Setting widget $widgetNum to $topText $bottomText")
    val data = mapOf(
        "push" to mapOf(
            "set" to mapOf(
                "widgetCustom${widgetNum}._.config.upper_text" to topText,
                "widgetCustom${widgetNum}._.config.lower_text" to bottomText
            )
        )
    )
    val serializedData = JSONObject(data).toString()
    Intent(SET_WIDGET_INTENT).apply {
        putExtra("EXTRA_CONFIG_JSON", serializedData)
    }.also {
        context.sendBroadcast(it)
    }
}

const val SET_WATCHFACE_INTENT: String = "nodomain.freeyourgadget.gadgetbridge.Q_SWITCH_WATCHFACE"
private fun setWatchFaceGadgetBridge(context: Context, watchFace: String) {
    Log.i("SetWatchFace", "Setting watchface to $watchFace")
    Intent(SET_WATCHFACE_INTENT).apply {
        putExtra("WATCHFACE_NAME", watchFace)
    }.also {
        context.sendBroadcast(it)
    }
}


