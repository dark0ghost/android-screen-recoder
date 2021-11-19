package org.dark0ghost.android_screen_recorder.controllers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import org.dark0ghost.android_screen_recorder.interfaces.GetsDirectory
import org.dark0ghost.android_screen_recorder.listeners.RListener
import org.dark0ghost.android_screen_recorder.services.RecordService
import org.dark0ghost.android_screen_recorder.states.BaseState
import org.dark0ghost.android_screen_recorder.time.CustomSubtitlesTimer
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.utils.setUiState
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordController(private val context: Context): GetsDirectory {
    private val rListener: RListener = RListener
        .Builder()
        .setCallbackOnFinalResult {
            setUiState(BaseState.DONE)
            val textFile = File(
                getsDirectory(),
                "${
                    SimpleDateFormat(
                        Settings.MainActivitySettings.FILE_NAME_FORMAT,
                        Locale.US
                    ).format(System.currentTimeMillis())
                }.srt"
            )
            textFile.writeText(buffer.toString().replace("[", "").replace("]", ""))
            Log.e("File/OnFinalResult", textFile.absoluteFile.toString())
            buffer.clear()
            subtitlesCounter = 0
            timer.stop()
            oldTime = "00:00:00"
        }
        .setCallbackOnTimeout {
            setUiState(BaseState.DONE)я
            if (speechStreamService != null) {
                speechStreamService = null
            }
        }
        .setCallbackOnResult {
            val template = """
            $subtitlesCounter
            $oldTime-->${timer.nowTime}    
            $it\n   
            """.trimIndent()
            this@setCallbackOnResult.buffer.add(template)
            Log.e("File/OnResult", template)
            subtitlesCounter++
            oldTime = timer.nowTime
        }я
        .build()
    private var recordService: RecordService? = null
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

    private var mBound: Boolean = false
    private var boundInlineButton: Boolean = true
    private var subtitlesCounter: Long = 1L
    private var oldTime: String = "00:00:00"я
    private val timer: CustomSubtitlesTimer = CustomSubtitlesTimer()


    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val metrics = context.resources.displayMetrics
            val binder = service as RecordService.RecordBinder
            recordService = binder.service.apply {
                setDpi(metrics.densityDpi)
            }
            Log.d("onServiceConnected", "init recordService{${recordService.hashCode()}}")
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    val connected: Boolean
        get() {
            return recordService != null
        }

    suspend fun startService(): Boolean {
        if (connected) return true
        val intent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_START_SERVICE
            putExtra(
                ScreenRecordingService.EXTRA_COMMAND_KEY,
                ScreenRecordingService.COMMAND_START_SERVICE
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        try {
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    this@ScreenRecordingServiceController.service =
                        (service as? ScreenRecordingService.ServiceBinder)?.service
                    this@ScreenRecordingServiceController.service?.setListener(serviceListener)
                    Log.d(LOG_TAG, "onServiceConnected() name ${name?.className}")
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    this@RecordController.recordService = null
                    Log.d(LOG_TAG, "onServiceDisconnected() name ${name?.className}")
                }
            }
            this.serviceConnection = serviceConnection
            context.bindService(
                Intent(context, ScreenRecordingService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return try {
            withTimeout(SERVICE_STARTING_TIMEOUT_MS) {
                while (isActive && !connected) {
                    delay(300L)
                }
                return@withTimeout connected
            }
        } catch (ex: Exception) {
            false
        }
    }

    // GetsDirectory

    override fun getsDirectory(): String {
        val rootDir = "${context.getExternalFilesDir("media")!!.absolutePath}/${Settings.MediaRecordSettings.NAME_DIR_SUBTITLE}/"
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
        Log.e("getsDirectory", rootDir)
        return rootDir
    }

    // End GetsDirectory

}