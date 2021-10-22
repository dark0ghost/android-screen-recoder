package org.dark0ghost.android_screen_recorder.services

import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import org.dark0ghost.android_screen_recorder.binders.RecordBinder


class RecordService: Service() {
    private val mediaProjection: MediaProjection? = null

    private var virtualDisplay: VirtualDisplay? = null

    private var running = false
    private var mediaRecorder: MediaRecorder? = null
    private var width = 720
    private var height = 1080
    private var dpi = 0

    private fun createVirtualDisplay() {
        mediaProjection?.let {
            virtualDisplay = it.createVirtualDisplay(
                "MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder!!.surface, null, null
            )
        }
    }

    fun setConfig(width1: Int, height1: Int, dpi1: Int) {
        width = width1
        height = height1
        dpi = dpi1
    }

    fun stopRecord(): Boolean {
        if (!running) {
            return false
        }
        running = false
        mediaRecorder?.apply {
            stop()
            reset()
        }
        virtualDisplay?.release()
        mediaProjection?.stop()
        return true
    }

    override fun onCreate() {
        super.onCreate()
        val serviceThread = HandlerThread(
            "service_thread",
            Process.THREAD_PRIORITY_BACKGROUND
        )
        serviceThread.start()
        running = false
        mediaRecorder = MediaRecorder()
    }

    override fun onBind(intent: Intent): IBinder {
        return  RecordBinder()
    }

}