package net.leloubil.fossilwidgets.widgetsapi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.leloubil.fossilwidgets.widgets.WidgetContent


fun TextWidget(topText: String, bottomText: String) = makeProvider {
    MutableStateFlow(
        WidgetContent(
            topText,
            bottomText
        )
    )
}
