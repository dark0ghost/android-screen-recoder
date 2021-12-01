package org.dark0ghost.android_screen_recorder.manager

import android.app.Service
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import org.dark0ghost.android_screen_recorder.interfaces.GetsDirectory
import org.dark0ghost.android_screen_recorder.interfaces.ScreenRecordListenerInterface
import org.dark0ghost.android_screen_recorder.states.RecordingState
import org.dark0ghost.android_screen_recorder.utils.Settings
import java.io.File
import java.io.IOException

class ScreenRecordManager(
    private val context: Context,
    private var listener: ScreenRecordListenerInterface
): GetsDirectory {
    //https://github.com/metalichesky/AndroidScreenRecorder/blob/master/app/src/main/java/com/metalichesky/screenrecorder/util/record/ScreenRecordManager.kt


    private var mediaProjectionCallback: MediaProjection.Callback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopRecording(false)
            }
        }

    private var mediaRecorder: MediaRecorder? = null
    private var projectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var dpi: Int = 0

    private fun createVirtualDisplay() {
        try {
            mediaProjection?.let {
                virtualDisplay = it.createVirtualDisplay(
                    "MainScreen",
                    Settings.MediaRecordSettings.WIDTH,
                    Settings.MediaRecordSettings.HEIGHT,
                    dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder?.surface,
                    null,
                    null
                )
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun destroyVirtualDisplay() {
        virtualDisplay?.release()
        virtualDisplay = null
    }

    private fun destroyMediaProjection() {
        Log.d("destroyMediaProjection", "destroy")
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun destroyRecorder() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (ex: Exception) {
            Log.e("$this:destroyRecorder", ex.message, ex)
        }
        mediaRecorder = null
    }

    private fun initRecorder() {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(Settings.MediaRecordSettings.AUDIO_SOURCE)
            setVideoSource(Settings.MediaRecordSettings.VIDEO_SOURCE)
            setOutputFormat(Settings.MediaRecordSettings.OUTPUT_FORMAT)
            setVideoSize(Settings.MediaRecordSettings.WIDTH, Settings.MediaRecordSettings.HEIGHT)
            setVideoEncoder(Settings.MediaRecordSettings.VIDEO_ENCODER)
            setAudioEncoder(Settings.MediaRecordSettings.AUDIO_ENCODER)
            setVideoEncodingBitRate(Settings.MediaRecordSettings.BIT_RATE)
            setVideoFrameRate(Settings.MediaRecordSettings.VIDEO_FRAME_RATE)
            setOutputFile("${getsDirectory()}${System.currentTimeMillis()}.mp4")
            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }


    init {
        projectionManager =
            context.getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    var recordState: RecordingState = RecordingState.IDLE
        private set(value: RecordingState){
            field = value
            val oldState = recordState
            field = value
            if (oldState != value) {
                listener.onRecordStateChanged(value)
            }
        }

    fun isRecording(): Boolean {
        return recordState == RecordingState.RECORDING
    }

    fun isMediaProjectionConfigured(): Boolean {
        return mediaProjection != null
    }

    fun isConfigured(): Boolean {
        return mediaRecorder != null && projectionManager != null
    }

    fun startRecording(running: Boolean): Boolean {
        Log.d(this.toString(), "start record")
        if (mediaProjection == null || running) {
            return false
        }
        initRecorder()
        createVirtualDisplay()
        recordState = RecordingState.RECORDING
        mediaRecorder?.start()
        return true
    }

    fun stopRecording(destroyMediaProjection: Boolean = false) {
        recordState = RecordingState.IDLE
        destroyRecorder()
        destroyVirtualDisplay()
        mediaProjection?.stop()
        if (destroyMediaProjection)
            destroyMediaProjection()
    }

    override fun getsDirectory(): String {
        val rootDir = "${context.getExternalFilesDir("media")!!.absolutePath}/${Settings.MediaRecordSettings.NAME_DIR_VIDEO}/"
        val file = File(rootDir)
        if (!file.exists()) {
            Log.e(
                "getsDirectory/mkdirs", if (file.mkdirs()) {
                    "path is created"
                } else {
                    "path isn't create"
                }
            )
        }
        if (Settings.DebugSettings.DEBUG_MODE) {
            Toast.makeText(context.applicationContext, rootDir, Toast.LENGTH_SHORT).show()
        }
        Log.i("getsDirectory", "${this::class.simpleName}: $rootDir")
        return rootDir
    }

}