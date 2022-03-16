package org.dark0ghost.android_screen_recorder.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.dark0ghost.android_screen_recorder.R
import org.dark0ghost.android_screen_recorder.interfaces.GetIntent
import org.dark0ghost.android_screen_recorder.interfaces.GetsDirectory
import org.dark0ghost.android_screen_recorder.interfaces.Prng
import org.dark0ghost.android_screen_recorder.states.RecordingState
import org.dark0ghost.android_screen_recorder.utils.ObjectRandom
import org.dark0ghost.android_screen_recorder.utils.Settings.DebugSettings.DEBUG_MODE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.ACTION_START_RECORDING
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.ACTION_START_SERVICE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.ACTION_STOP_SERVICE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.AUDIO_ENCODER
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.AUDIO_SOURCE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.BIT_RATE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.COMMAND_STOP_SERVICE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.HEIGHT
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.NAME_DIR_VIDEO
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.OUTPUT_FORMAT
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.SERVICE_THREAD_NAME
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.VIDEO_ENCODER
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.VIDEO_FRAME_RATE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.VIDEO_SOURCE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.WIDTH
import org.dark0ghost.android_screen_recorder.utils.Settings.NotificationSettings.CHANNEL_ID
import org.dark0ghost.android_screen_recorder.utils.Settings.NotificationSettings.CONTENT_TEXT
import org.dark0ghost.android_screen_recorder.utils.Settings.NotificationSettings.CONTENT_TITTLE
import org.dark0ghost.android_screen_recorder.utils.Settings.NotificationSettings.NOTIFICATION_FOREGROUND_ID
import org.dark0ghost.android_screen_recorder.utils.closeServiceNotification
import java.io.File
import java.io.IOException


class RecordService: GetsDirectory, Service() {
    private val binder = RecordBinder()
    private val prng: Prng = ObjectRandom()
    private val contentText: String
        get() =
             resources.getString(CONTENT_TEXT)
    private val contentTitle: String
        get() =
            resources.getString(CONTENT_TITTLE)
    private val mediaProjectionCallback: MediaProjection.Callback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopRecord()
            }
        }

    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null

    private var dpi: Int = 0
    private var notificationId: Int = 0
    private var recordingState: RecordingState = RecordingState.IDLE

    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var projectionManager: MediaProjectionManager

    private fun createVirtualDisplay() {
        try {
            mediaProjection?.let {
                virtualDisplay = it.createVirtualDisplay(
                    "MainScreen",
                    WIDTH,
                    HEIGHT,
                    dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.surface,
                    null,
                    null
                )
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): NotificationChannel =
        NotificationChannel(CHANNEL_ID, contentText, NotificationManager.IMPORTANCE_DEFAULT)

    private fun initNotification(): NotificationCompat.Builder {
        val notificationBuilder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationCompat.Builder(this, CHANNEL_ID).apply {
                    setContentTitle(contentTitle)
                    setContentText(contentText)
                    setSmallIcon(R.drawable.ic_notification_custom)
                }
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                NotificationCompat.Builder(this, "").apply {
                    setContentTitle(contentTitle)
                    setContentText(contentText)
                    setSmallIcon(R.drawable.ic_stat_cast_connected)
                }
            }
        return notificationBuilder
    }

    private fun initRecorder() {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(AUDIO_SOURCE)
            setVideoSource(VIDEO_SOURCE)
            setOutputFormat(OUTPUT_FORMAT)
            setVideoSize(WIDTH, HEIGHT)
            setVideoEncoder(VIDEO_ENCODER)
            setAudioEncoder(AUDIO_ENCODER)
            setVideoEncodingBitRate(BIT_RATE)
            setVideoFrameRate(VIDEO_FRAME_RATE)
            setOutputFile("${getsDirectory()}/${System.currentTimeMillis()}.mp4")
            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun createServiceNotificationBuilder(context: Context): NotificationCompat.Builder {

        val notificationBuilder =
            NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setContentTitle(contentTitle)
                setContentText(contentText)
                setSmallIcon(R.drawable.ic_notification_custom)
            }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(createNotificationChannel())
        }
        return notificationBuilder
    }

    private fun updateServiceNotification(
        context: Context
    ) {
        val startIntent = Intent(this, this::class.java)
        startIntent.action = ACTION_START_RECORDING
        val closeIntent = Intent(this, this::class.java)
        closeIntent.action = ACTION_STOP_SERVICE
        val closePendingIntent = PendingIntent.getService(
            context, COMMAND_STOP_SERVICE, closeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        Log.d("update notification", "state: $recordingState")
        when (recordingState) {
            RecordingState.IDLE -> {
                val builder = initNotification()
                    .addAction(
                    R.drawable.ic_close,
                    getString(R.string.close),
                    closePendingIntent
                )
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_FOREGROUND_ID, builder.build())
            }
            RecordingState.PREPARED -> {
                val builder = initNotification().apply {
                    addAction(
                        R.drawable.ic_close,
                        getString(R.string.close),
                        closePendingIntent
                    )
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_FOREGROUND_ID, builder.build())
            }
            RecordingState.RECORDING -> {
                val builder = initNotification().apply {
                    addAction(
                        R.drawable.ic_close,
                        getString(R.string.close),
                        closePendingIntent
                    )
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_FOREGROUND_ID, builder.build())
            }
        }
    }

    private fun destroyMediaProjection() {
        Log.d("destroyMediaProjection", "destroy")
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun recorderStartServiceWithId(startId: Int) {
        Log.d("StartServiceWithId", "startService() startId = $startId")
        notificationId = prng.randomNumber(0, Int.MAX_VALUE)
        val closeIntent = Intent(this, this::class.java)
        closeIntent.action = ACTION_STOP_SERVICE
        val closePendingIntent = PendingIntent.getService(
            this, COMMAND_STOP_SERVICE, closeIntent,  PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // setting the mutability flag
        )
        startForeground(
            notificationId,
            createServiceNotificationBuilder(applicationContext).addAction(
                R.drawable.ic_close,
                getString(R.string.close),
                closePendingIntent
            ).build()
        )
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    var running: Boolean = false
        private set

    fun setupMediaProjection(params: MediaProjection) {
        mediaProjection = params
        mediaProjection?.registerCallback(mediaProjectionCallback, null)
    }

    fun isMediaProjectionConfigured(): Boolean {
        return mediaProjection != null
    }

    fun setDpi(dpi1: Int) {
        dpi = dpi1
    }


    fun startRecord(): Boolean {
        Log.d("$this:startRecord", "start record")
        if (mediaProjection == null || running) {
            Log.e("$this:mediaProjection", "is null")
            return false
        }
        initRecorder()
        createVirtualDisplay()
        recordingState = RecordingState.RECORDING
        updateServiceNotification(this)
        mediaRecorder.start()
        running = true
        return true
    }

    fun stopRecord(): Boolean {
        if (!running) {
            return false
        }
        recordingState = RecordingState.IDLE
        updateServiceNotification(this)
        running = false
        mediaRecorder.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                return true
            }
            reset()
            release()
        }
        virtualDisplay?.release()
        mediaProjection?.stop()
        destroyMediaProjection()
        return true
    }

    // GetsDirectory

    override fun getsDirectory(): String {
        val rootDir = "/storage/emulated/0/DCIM/screen"//"/storage/emulated/0/${Environment.DIRECTORY_DOWNLOADS}/screen"
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
        if (DEBUG_MODE) {
            Toast.makeText(applicationContext, rootDir, Toast.LENGTH_SHORT).show()
        }
        Log.i("getsDirectory", "${this::class.simpleName}: $rootDir")
        return rootDir
    }

    // End GetsDirectory

    override fun onCreate() {
        super.onCreate()
        val serviceThread = HandlerThread(
            SERVICE_THREAD_NAME,
            Process.THREAD_PRIORITY_BACKGROUND
        )
        serviceThread.start()
        running = false
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = if (intent.action != null) {
            intent.action
        } else {
            return START_NOT_STICKY
        }
        Log.e("onStartCommand", "action: $action")
        when (action) {
            ACTION_START_SERVICE -> {
                recorderStartServiceWithId(startId)
            }
            ACTION_START_RECORDING -> {
                startRecord()
            }
            ACTION_STOP_SERVICE -> {
                closeServiceNotification(this, NOTIFICATION_FOREGROUND_ID)
                stopForeground(true)
            }
        }
        return START_NOT_STICKY
    }


    inner class RecordBinder : Binder() {
        internal val service: RecordService
            get() = this@RecordService
    }

    companion object : GetIntent {
        override fun intent(context: Context): Intent = Intent(context, RecordService::class.java)
    }
}
