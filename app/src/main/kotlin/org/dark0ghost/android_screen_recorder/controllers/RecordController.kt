package org.dark0ghost.android_screen_recorder.controllers

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.hbisoft.hbrecorder.HBRecorderListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import org.dark0ghost.android_screen_recorder.interfaces.Controller
import org.dark0ghost.android_screen_recorder.interfaces.GetsDirectory
import org.dark0ghost.android_screen_recorder.manager.RecorderManager
import org.dark0ghost.android_screen_recorder.services.RecordService
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.ACTION_START_SERVICE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.COMMAND_START_SERVICE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.EXTRA_COMMAND_KEY
import org.dark0ghost.android_screen_recorder.utils.Settings.RecorderControllerSettings.SERVICE_STARTING_TIMEOUT_MS
import java.io.File

class RecordController(private val context: Context): Controller {
    private var recordService: RecordService? = null

    private val connection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val metrics = context.resources.displayMetrics
            val binder = service as RecordService.RecordBinder
            recordService = binder.service.apply {
                setDpi(metrics.densityDpi)
            }
            Log.d("onServiceConnected", "init recordService{${recordService.hashCode()}}")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            this@RecordController.recordService = null
        }
    }

    val isMediaProjectionConfigured: Boolean
    get(){
        recordService?.let{
           return it.isMediaProjectionConfigured()
        }
        return false
    }

    fun setupMediaProjection(localMediaProjection: MediaProjection) {
        recordService?.let{
            it.setupMediaProjection(localMediaProjection)
            Log.d("setupMediaProjection", "is setup")
            return
        }
        Log.d("setupMediaProjection", "not setup")
    }

    fun setRecorder(context: Context, listener: HBRecorderListener) {
        recordService?.recorderManager  = RecorderManager(context, listener)
    }

    fun setActivity(activity: Activity) {
        recordService?.activity = activity
    }

    override val connected: Boolean
        get() {
            return recordService != null
        }

    override fun startRecording(data: Intent, resultCode: Int, activity: Activity) {
        recordService?.startRecord(data, resultCode, activity)
    }

    override fun stopRecording() {
        recordService?.stopRecord()
    }

    override fun close() {
        stopService()
    }

    override fun stopService(): Boolean {
        if (!connected) return true
        try {
            context.unbindService(connection)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        context.stopService(Intent(context, RecordService::class.java))
        return true
    }

    override suspend fun startService(): Boolean {
        Log.d("$this:startService", "start")
        if (connected) return true
        Log.d("$this:startService", "not connected")
        val intent = RecordService.intent(context).apply {
            action = ACTION_START_SERVICE
            putExtra(
                EXTRA_COMMAND_KEY,
                COMMAND_START_SERVICE
            )
        }
        context.startForegroundService(intent)
        try {
            context.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return true
    }
}