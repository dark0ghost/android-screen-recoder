package org.dark0ghost.android_screen_recorder.interfaces

interface ScreenRecordingServiceListener {
    fun onRecordingStarted()
    fun onRecordingStopped(filePath: String?)
    fun onNeedSetupMediaProjection()
    fun onNeedSetupMediaRecorder()
    fun onServiceClosed()
}