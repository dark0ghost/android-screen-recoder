package org.dark0ghost.android_screen_recorder.ui.composable

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.dark0ghost.android_screen_recorder.states.ClickState
import org.dark0ghost.android_screen_recorder.ui.theme.CustomDarkTheme
import org.dark0ghost.android_screen_recorder.utils.GradientButton
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.utils.Settings.ComposeSettings.GRADIENT

@Preview(name = "MainUIPreview", group = "Main",
    device = "spec:shape=Normal,width=1080,height=1920,unit=px,dpi=640",
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun MainUI(clickButtonCallback: () -> ClickState = {
    Log.d("default func", "test")
    ClickState.NotUsed
}
) {
    var isClicked by rememberSaveable {
        mutableStateOf(ClickState.NotClicked)
    }
    CustomDarkTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GradientButton(
                text = when (isClicked) {
                    ClickState.IsClicked -> stringResource(Settings.ButtonText.STOP_RECORD_TEXT_ID)
                    else -> stringResource(Settings.ButtonText.START_RECORD_TEXT_ID)
                },
                gradient = GRADIENT,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                isClicked = clickButtonCallback()
            }
        }
    }
}