package org.dark0ghost.android_screen_recorder.controllers

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import org.dark0ghost.android_screen_recorder.interfaces.Controller
import org.dark0ghost.android_screen_recorder.services.SpeechService
import org.dark0ghost.android_screen_recorder.utils.Settings

class SpeechController(private val context: Context) : Controller {
    private val connection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SpeechService.SpeechBinder
            speechService = binder.service
            Log.e("onServiceConnected", "init SpeechService{${speechService.hashCode()}}")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            this@SpeechController.speechService = null
        }
    }

    private var speechService: SpeechService? = null

    override val connected: Boolean
        get() {
            return speechService != null
        }

    override fun startRecording(data: Intent, resultCode: Int, activity: Activity) {
        speechService?.start(context)
    }

    override fun stopRecording() {
        Log.e("speechService", (speechService == null).toString())
        speechService?.stop()
    }

    override fun close() {
        speechService?.close()
        stopService()
    }

    override fun stopService(): Boolean {

        try {
            context.unbindService(connection)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        context.stopService(Intent(context, SpeechService::class.java))
        return true
    }

    override suspend fun startService(): Boolean {
        Log.d("$this:startService", "start")
        if (connected) return true
        Log.d("$this:startService", "not connected")
        val intent = SpeechService.intent(context).apply {
            action = Settings.MediaRecordSettings.ACTION_START_SERVICE
        }
        context.startService(intent)
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