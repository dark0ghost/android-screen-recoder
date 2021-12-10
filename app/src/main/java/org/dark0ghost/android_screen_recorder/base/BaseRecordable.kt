package org.dark0ghost.android_screen_recorder.base

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dark0ghost.android_screen_recorder.controllers.RecordController
import org.dark0ghost.android_screen_recorder.controllers.SpeechController
import org.dark0ghost.android_screen_recorder.interfaces.Recordable
import org.dark0ghost.android_screen_recorder.states.ClickState
import org.dark0ghost.android_screen_recorder.utils.*

abstract class BaseRecordable: AppCompatActivity() {
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (isPermissionsGranted(result)) {
            tryStartRecording()
        }
    }

    private val recordScreenLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            val mediaProjectionMain =
                projectionManager.getMediaProjection(
                    activityResult.resultCode,
                    activityResult.data ?: Intent()
                )
            serviceController.setupMediaProjection(mediaProjectionMain)
            tryStartRecording()
        } else {
            Toast.makeText(
                this,
                "Screen Cast Permission Denied", Toast.LENGTH_SHORT
            ).show()
        }
    }

    protected lateinit var projectionManager: MediaProjectionManager
    protected lateinit var serviceController: RecordController
    protected lateinit var speechController: SpeechController

    protected lateinit var listRecordable: List<Recordable>

    private fun tryStartRecording() {
        lifecycleScope.launch {
            while (isActive && !serviceController.connected) {
                Log.d("tryStartRecording", "start service")
                serviceController.startService()
                delay(100)
            }
            if (!isActive) return@launch

            val permissions =
                Settings.PermissionsSettings.RECORD_AUDIO_PERMISSIONS
            val permissionsGranted = isPermissionsGranted(this@BaseRecordable, permissions)
            Log.d(
                "tryStartRecording",
                "isMediaProjectionConfigured:${serviceController.isMediaProjectionConfigured}"
            )
            if (permissionsGranted && serviceController.isMediaProjectionConfigured) {
                Log.d("tryStartRecording", "start record")
                startRecording()
            } else if (!permissionsGranted) {
                Log.d("tryStartRecording", "get permissions")
                permissionsLauncher.launch(permissions)
            } else if (!serviceController.isMediaProjectionConfigured) {
                Log.d("tryStartRecording", "launch")
                recordScreenLauncher.launch(getScreenCaptureIntent(this@BaseRecordable))
            }
        }
    }

    private fun startRecording() = listRecordable.forEach {
        startRecordable(it)
    }


    private fun stopRecording() = listRecordable.forEach {
        stopRecordable(it)
    }

    protected var isStartRecord: ClickState = ClickState.NotClicked

    protected fun clickButton() {
        Log.d("clickButton", "start")
        when (isStartRecord) {
            ClickState.NotClicked -> {
                Log.d("clickButton", "start record")
                isStartRecord = ClickState.IsClicked
                tryStartRecording()
                return
            }
            ClickState.IsClicked -> {
                Log.d("clickButton", "stop record")
                stopRecording()
                isStartRecord = ClickState.NotClicked
                return
            }
            else -> Log.e("clickButton", "isStartRecord have state:$isStartRecord, this is ok?")
        }
    }

    protected fun initService() {
        Log.d("initService", "init")
        serviceController = RecordController(this)
        speechController = SpeechController(this)
        listRecordable = listOf<Recordable>(speechController, serviceController)
        lifecycleScope.launch {
            while (isActive && (!serviceController.connected || !speechController.connected)) {
                Log.d("initService", "start service")
                println(serviceController.connected)
                println(speechController.connected)
                if (!serviceController.connected)
                    serviceController.startService()
                if (!speechController.connected)
                    speechController.startService()
            }
        }
    }
}