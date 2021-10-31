package org.dark0ghost.android_screen_recorder.interfaces

import android.content.Context
import android.content.Intent

fun interface GetIntent {
        fun intent(context: Context): Intent
}