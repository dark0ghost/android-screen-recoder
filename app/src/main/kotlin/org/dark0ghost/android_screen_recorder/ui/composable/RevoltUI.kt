package org.dark0ghost.android_screen_recorder.ui.composable

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import org.dark0ghost.android_screen_recorder.R
import org.dark0ghost.android_screen_recorder.states.ClickState


@Preview(name = "MainUIPreview", group = "Overlay",
    device = "spec:shape=Normal,width=1440,height=3040,unit=px,dpi=640",
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL, apiLevel = 31
)
@Composable
fun RevoltUi(modifier: Modifier  = Modifier,clickState: MutableState<ClickState> = mutableStateOf(ClickState.NotUsed)){
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        IconButton(onClick = { }) {
            Icon(
                painter = painterResource(id = R.drawable.pause),
                contentDescription = null
            )
        }
    }
}

