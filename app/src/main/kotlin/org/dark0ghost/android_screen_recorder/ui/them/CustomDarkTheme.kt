package org.dark0ghost.android_screen_recorder.ui.them

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.dark0ghost.android_screen_recorder.R
import org.dark0ghost.android_screen_recorder.utils.ImageWithBackground

@Preview
@Composable
fun CustomDarkTheme(content: @Composable () -> Unit = {}) {
    ImageWithBackground(
        backgroundDrawableResId = R.drawable.dark_night,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .fillMaxSize()
    )
    CompositionLocalProvider {
        MaterialTheme(
            content = content
        )
    }
}