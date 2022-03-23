package org.dark0ghost.android_screen_recorder.controllers

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
            Log.d("onServiceConnected", "init recordService{${speechService.hashCode()}}")
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

    override fun startRecording() {
        speechService?.start(context)
    }

    override fun stopRecording() {
        speechService?.stop()
    }

    override fun close() {
        speechService?.close()
        stopService()
    }

    override fun stopService(): Boolean {
        if (!connected) return true
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
        val intent = SpeechService.intent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        try {
            context.bindService(
                SpeechService.intent(context),
                connection,
                Context.BIND_AUTO_CREATE
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return try {
            withTimeout(Settings.RecorderControllerSettings.SERVICE_STARTING_TIMEOUT_MS) {
                while (isActive && !connected) {
                    delay(300L)
                }
                return@withTimeout connected
            }
        } catch (ex: Exception) {
            false
        }
    }
}