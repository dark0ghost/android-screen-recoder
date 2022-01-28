package org.dark0ghost.android_screen_recorder.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.dark0ghost.android_screen_recorder.utils.ImageWithBackground
import org.dark0ghost.android_screen_recorder.R

@Preview
@Composable
fun MainUI() {
    ImageWithBackground(
        painter = painterResource(id = R.drawable.ic_notification_custom),
        backgroundDrawableResId = R.drawable.dark_night,
        contentDescription = "",
        modifier = Modifier
            .height(160.dp)
            .width(160.dp)
            .padding(32.dp),
    )
}