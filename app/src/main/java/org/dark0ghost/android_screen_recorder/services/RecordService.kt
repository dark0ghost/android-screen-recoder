package org.dark0ghost.android_screen_recorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.*
import android.widget.Toast
import androidx.drawerlayout.R
import org.dark0ghost.android_screen_recorder.interfaces.GetIntent
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.BIT_RATE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.HEIGHT
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.NAME_DIR
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.SERVICE_THREAD_NAME
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.VIDEO_FRAME_RATE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.WIDTH
import org.dark0ghost.android_screen_recorder.utils.Settings.NotificationSettings.CHANNEL_ID
import org.dark0ghost.android_screen_recorder.utils.Settings.NotificationSettings.CONTENT_TEXT
import org.dark0ghost.android_screen_recorder.utils.Settings.NotificationSettings.CONTENT_TITTLE
import org.dark0ghost.android_screen_recorder.utils.Settings.NotificationSettings.FOREGROUND_ID
import java.io.File
import java.io.IOException


open class RecordService: Service() {

    private val binder = RecordBinder()

    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var width = WIDTH
    private var height = HEIGHT
    private var dpi = 0

    private fun createVirtualDisplay() {
        mediaProjection?.let {
            virtualDisplay = it.createVirtualDisplay(
                "MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder!!.surface, null, null
            )
        }
    }

    private fun getsDirectory(): String {
        val rootDir = "${externalCacheDir!!.absolutePath}/${NAME_DIR}/"
        val file = File(rootDir)
        if (!file.exists()) {
            file.mkdirs()
        }
        Toast.makeText(applicationContext, rootDir, Toast.LENGTH_SHORT).show()
        return rootDir

    }

    private fun initRecorder() {
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile("${getsDirectory()}${System.currentTimeMillis()}.mp4")
            setVideoSize(width, height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setVideoEncodingBitRate(BIT_RATE)
            setVideoFrameRate(VIDEO_FRAME_RATE)
            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun initNotification(): Notification {
        val notificationBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID).apply {
                    setContentTitle(CONTENT_TITTLE)
                    setContentText(CONTENT_TEXT)
                    setSmallIcon(R.drawable.notification_icon_background)
                }
            } else {
                Notification.Builder(this).apply {
                    setContentTitle(CONTENT_TITTLE)
                    setContentText(CONTENT_TEXT)
                    setPriority(Notification.PRIORITY_DEFAULT)
                    setSmallIcon(R.drawable.notification_icon_background)
                }
            }
        return notificationBuilder.build()
    }

    open var running = false

    var mediaProjection: MediaProjection? = null

    fun setConfig(width1: Int, height1: Int, dpi1: Int) {
        width = width1
        height = height1
        dpi = dpi1
    }

    fun startRecord(): Boolean {
        if (mediaProjection == null || running) {
            return false
        }
        initRecorder()
        createVirtualDisplay()
        mediaRecorder?.start()
        running = true
        return true
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
            SERVICE_THREAD_NAME,
            Process.THREAD_PRIORITY_BACKGROUND
        )
        serviceThread.start()
        running = false
        mediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = initNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, CONTENT_TEXT, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationManager.notify(FOREGROUND_ID, notification)
        startForeground(FOREGROUND_ID, notification)
        return START_NOT_STICKY
    }


    inner class RecordBinder : Binder() {
        fun getRecordService(): RecordService = this@RecordService
    }

    companion object: GetIntent {
        override fun intent(context: Context): Intent {
            return Intent(context, RecordService::class.java)
        }
    }
}