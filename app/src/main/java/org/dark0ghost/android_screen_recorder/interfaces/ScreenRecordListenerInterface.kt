package org.dark0ghost.android_screen_recorder.interfaces

import org.dark0ghost.android_screen_recorder.states.RecordingState

interface ScreenRecordListenerInterface {
    fun onRecordStarted()
    fun onRecordStopped(filePath: String)
    fun onRecordStateChanged(state: RecordingState)
    fun onNeedSetupMediaProjection()
    fun onNeedSetupMediaRecorder()
}