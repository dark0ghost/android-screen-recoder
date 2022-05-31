package org.dark0ghost.android_screen_recorder.interfaces

import android.app.Activity
import android.content.Intent

interface Recordable {
    fun startRecording(data: Intent = Intent(), resultCode: Int = -1, activity: Activity = Activity())

    fun stopRecording()

    fun close()
}