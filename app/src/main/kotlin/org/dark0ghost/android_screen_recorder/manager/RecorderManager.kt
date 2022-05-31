package org.dark0ghost.android_screen_recorder.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener


class RecorderManager(context: Context, listener: HBRecorderListener) {

    var path: String = ""
    private val hbRecorder: HBRecorder = HBRecorder(context, listener)

    fun startRecord(data: Intent, resultCode: Int, activity: Activity) {
        hbRecorder.startScreenRecording(data, resultCode, activity)
        hbRecorder.setOutputPath(path)
    }

    fun stopRecord() {
        hbRecorder.stopScreenRecording()
    }
}