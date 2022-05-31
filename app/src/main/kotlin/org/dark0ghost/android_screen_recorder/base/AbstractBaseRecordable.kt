package org.dark0ghost.android_screen_recorder.base

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.hbisoft.hbrecorder.HBRecorderListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dark0ghost.android_screen_recorder.R
import org.dark0ghost.android_screen_recorder.controllers.RecordController
import org.dark0ghost.android_screen_recorder.controllers.SpeechController
import org.dark0ghost.android_screen_recorder.interfaces.Recordable
import org.dark0ghost.android_screen_recorder.utils.*
import org.dark0ghost.android_screen_recorder.states.ClickState
import org.dark0ghost.android_screen_recorder.ui.activity.MainActivity

abstract class AbstractBaseRecordable: AppCompatActivity() {
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (isPermissionsGranted(result)) {
            recordScreenLauncher.launch(getScreenCaptureIntent(this@AbstractBaseRecordable))
        } else {
            var arrayPermission = arrayOf(
                Manifest.permission.RECORD_AUDIO,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                arrayPermission = arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            }

            ActivityCompat.requestPermissions(
                this,
                arrayPermission,
                Settings.AudioRecordSettings.PERMISSIONS_REQUEST_RECORD_AUDIO
            )
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
            serviceController.setRecorder(this as Context, object : HBRecorderListener {
                override fun HBRecorderOnStart() = Unit

                override fun HBRecorderOnComplete() = Unit

                override fun HBRecorderOnError(errorCode: Int, reason: String?)  = Unit
            })
            serviceController.setActivity(this)
            try {
                tryStartRecording(activityResult.data?: Intent(), activityResult.resultCode)
            } catch (e : IllegalStateException) {
                Log.e("recoder", "no record", e)
            }
        } else {
            Toast.makeText(
                this,
                "Screen Cast Permission Denied", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private lateinit var inlineButton: ImageButton

    protected lateinit var projectionManager: MediaProjectionManager
    protected lateinit var serviceController: RecordController
    protected lateinit var speechController: SpeechController
    protected lateinit var listRecordable: List<Recordable>

    protected var isStartRecord: ClickState = ClickState.NotClicked

    private fun tryStartRecording(data: Intent, resultCode: Int) {
        lifecycleScope.launch {
            while (isActive && !serviceController.connected) {
                Log.d("tryStartRecording", "start service")
                serviceController.startService()
            }
            if (!isActive) return@launch

            val permissions =
                Settings.PermissionsSettings.RECORD_AUDIO_PERMISSIONS
            val permissionsGranted = isPermissionsGranted(this@AbstractBaseRecordable, permissions)
            Log.d(
                "tryStartRecording",
                "isMediaProjectionConfigured:${serviceController.isMediaProjectionConfigured}"
            )
            if (permissionsGranted && serviceController.isMediaProjectionConfigured) {
                Log.d("tryStartRecording", "start record")
                startRecording(data, resultCode, this@AbstractBaseRecordable)
            } else if (!permissionsGranted) {
                Log.d("tryStartRecording", "get permissions")
                permissionsLauncher.launch(permissions)
            } else if (!serviceController.isMediaProjectionConfigured) {
                Log.d("tryStartRecording", "launch")
                recordScreenLauncher.launch(getScreenCaptureIntent(this@AbstractBaseRecordable))
            }
        }
    }

    protected open fun startRecording(data: Intent, resultCode: Int, activity: Activity) {
        inlineButton.setImageResource(R.drawable.pause)
        listRecordable.forEach {
            startRecordable(it, data, resultCode, activity)
        }
    }


    protected open fun stopRecording() {
        inlineButton.setImageResource(R.drawable.recording_64)
        listRecordable.forEach {
            stopRecordable(it)
        }
    }

    protected fun clickButton(): ClickState {
        Log.d("clickButton", "start")
        when (isStartRecord) {
            ClickState.NotClicked -> {
                Log.d("clickButton", "start record")
                isStartRecord = ClickState.IsClicked
                recordScreenLauncher.launch(getScreenCaptureIntent(this@AbstractBaseRecordable))
                return ClickState.IsClicked
            }
            ClickState.IsClicked -> {
                Log.e("clickButton", "stop record")
                stopRecording()
                isStartRecord = ClickState.NotClicked
                return ClickState.NotClicked
            }
            else -> Log.e("clickButton", "isStartRecord have state:$isStartRecord, this is ok?")
        }
        return  ClickState.NotUsed
    }

    protected fun initService() {
        Log.d("initService", "init")
        serviceController = RecordController(this)
        speechController = SpeechController(this)
        listRecordable = listOf<Recordable>(speechController, serviceController)
        lifecycleScope.launch {
            Log.d("initService", "start service")
            if (!serviceController.connected) {
               serviceController.startService()
            }
            if (!speechController.connected) {
               speechController.startService()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nullParent: ViewGroup? = null
        val topView = LayoutInflater.from(this).inflate(R.layout.revolt, nullParent) as RelativeLayout
        inlineButton = topView.findViewById(R.id.grub)
    }
}