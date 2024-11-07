package net.leloubil.fossilwidgets

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import net.leloubil.fossilwidgets.ui.theme.FossilWidgetsTheme
import net.leloubil.fossilwidgets.widgets.FakeWidgetService
import net.leloubil.fossilwidgets.widgets.WatchWidgetService
import net.leloubil.fossilwidgets.widgets.WidgetsManager
import net.leloubil.fossilwidgets.stateproviders.NotificationListener
import net.leloubil.fossilwidgets.widgets.RealWidgetService

const val USE_FAKE_WIDGET_SERVICE: Boolean = false
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}



@Composable
fun ButtonsList(modifier: Modifier = Modifier, widgetService: WatchWidgetService) {
    val context = LocalContext.current
    Column(modifier) {
        LaunchedEffect(widgetService) {
            WidgetsManager.widgetService = widgetService
            // check for android calendar permission
            Log.i(
                "NotificationListener",
                "Notification listener enabled: ${NotificationListener.isEnabled(context)}"
            )
            if (!NotificationListener.isEnabled(context)) {
                Log.i("FossilWidgets", "Requesting notification listener permission")
                // request permission
                context.startActivity(
                    Intent(
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                    )
                );
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CALENDAR
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("FossilWidgets", "Requesting calendar permission")
                // request permission
                ActivityCompat.requestPermissions(
                    context as ComponentActivity,
                    arrayOf(android.Manifest.permission.READ_CALENDAR),
                    0
                )
            }
        }
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                Log.i("FossilWidgets", "Set widget count")
                WidgetsManager.setWatchface(BaseWatchFace(), context)
            }) {
            Text("Reset all")
        }
    }
}


@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun App() {
    FossilWidgetsTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Component(innerPadding)
        }
    }
}

@Composable
private fun Component(innerPadding: PaddingValues) {
    Column(modifier = Modifier.padding(innerPadding)) {
        val fakeWidgetService by remember { mutableStateOf(FakeWidgetService()) }
        ButtonsList(widgetService = if (USE_FAKE_WIDGET_SERVICE) fakeWidgetService else RealWidgetService)
        Column {
            Text("watchface: ${fakeWidgetService.watchFace}")
            Text("Widget count: ${fakeWidgetService.state.size}")
            fakeWidgetService.state.forEachIndexed { i, widgetContent ->
                Column {
                    Text("Widget $i")
                    Text("Top: ${widgetContent.topText}")
                    Text("Bottom: ${widgetContent.bottomText}")
                }
            }
        }
    }
}
