package org.dark0ghost.android_screen_recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dark0ghost.android_screen_recorder.controllers.RecordController
import org.dark0ghost.android_screen_recorder.controllers.SpeechController
import org.dark0ghost.android_screen_recorder.interfaces.GetsDirectory
import org.dark0ghost.android_screen_recorder.interfaces.Recordable
import org.dark0ghost.android_screen_recorder.services.ButtonService
import org.dark0ghost.android_screen_recorder.states.BaseState
import org.dark0ghost.android_screen_recorder.states.ClickState
import org.dark0ghost.android_screen_recorder.utils.*
import org.dark0ghost.android_screen_recorder.utils.Settings.AudioRecordSettings.PERMISSIONS_REQUEST_RECORD_AUDIO
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.callbackForStartRecord
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.isStartButton
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.NAME_DIR_SUBTITLE
import org.dark0ghost.android_screen_recorder.utils.Settings.Model.model
import org.dark0ghost.android_screen_recorder.utils.Settings.PermissionsSettings.RECORD_AUDIO_PERMISSIONS
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.android.StorageService
import java.io.BufferedWriter
import java.io.File
import java.io.IOException


class MainActivity : GetsDirectory, AppCompatActivity() {

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

    private var isStartRecord: ClickState = ClickState.NotClicked

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var startRecorder: Button
    private lateinit var buttonStartInlineButton: Button
    private lateinit var intentButtonService: Intent
    private lateinit var serviceController: RecordController
    private lateinit var speechController: SpeechController
    private lateinit var listRecordable: List<Recordable>

    private fun initModel() {
        val callbackModelInit = { models: org.vosk.Model ->
            model = models
            setUiState(BaseState.READY)
        }
        StorageService.unpack(
            this, "model_ru", "models", callbackModelInit
        ) { exception: IOException ->
            Log.e("init-model-fn", "Failed to unpack the model ${exception.printStackTrace()}")
        }
        Log.d("initModel", "run complete")
    }

    private fun initService() {
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

    private fun clickButton() {
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

    private fun checkPermissionsOrInitialize() {
        val permissionCheckRecordAudio =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        var arrayPermission = arrayOf(
            Manifest.permission.RECORD_AUDIO,
        )
        var checkPermission = permissionCheckRecordAudio != PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val permissionCheckForegroundService =
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            arrayPermission = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE
            )
            checkPermission =
                permissionCheckRecordAudio != PackageManager.PERMISSION_GRANTED || permissionCheckForegroundService != PackageManager.PERMISSION_GRANTED
        }
        if (checkPermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayPermission,
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return
        }
        initModel()
    }

    private fun startRecording() = listRecordable.forEach {
        startRecordable<Recordable>(it)
    }


    private fun stopRecording() = listRecordable.forEach {
        stopRecordable<Recordable>(it)
    }

    private fun tryStartRecording() {
        lifecycleScope.launch {
            while (isActive && !serviceController.connected) {
                Log.d("tryStartRecording", "start service")
                serviceController.startService()
                delay(100)
            }
            if (!isActive) return@launch

            val permissions =
                RECORD_AUDIO_PERMISSIONS
            val permissionsGranted = isPermissionsGranted(this@MainActivity, permissions)
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
                recordScreenLauncher.launch(getScreenCaptureIntent(this@MainActivity))
            }
        }
    }

    // GetsDirectory

    override fun getsDirectory(): String {
        val rootDir = "${getExternalFilesDir("media")!!.absolutePath}/${NAME_DIR_SUBTITLE}/"
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
        Log.i("getsDirectory", "${this::class.simpleName}: $rootDir")
        return rootDir
    }

    // End GetsDirectory


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        setUiState(BaseState.START)

        buttonStartInlineButton = findViewById(R.id.start_inline_button)

        buttonStartInlineButton.setOnClickListener {
            Log.d(
                "buttonStartInlineButton", if (isStartButton) {
                    "build button"
                } else {
                    "deleted button"
                }
            )
            if (isStartButton) {
                isStartButton = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !Settings.canDrawOverlays(
                        this
                    )
                ) {
                    intentButtonService = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intentButtonService)
                    return@setOnClickListener
                }
                intentButtonService = ButtonService.intent(this)
                startService(intentButtonService)
                return@setOnClickListener
            }
            try {
                stopService(intentButtonService)
            } catch (e: java.lang.IllegalArgumentException) {

            }
            isStartButton = true
            return@setOnClickListener
        }
        initService()

        callbackForStartRecord = callback@{
            clickButton()
            return@callback isStartRecord
        }

        startRecorder = findViewById(R.id.start_record)
        startRecorder.setOnClickListener {
            clickButton()
        }

        LibVosk.setLogLevel(LogLevel.INFO)

        checkPermissionsOrInitialize()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceController.close()
        if (::intentButtonService.isInitialized) // check for AndroidTest (android test not start onCreate)
            stopService(intentButtonService)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initModel()
                return
            }
            finish()
        }
    }

    companion object {
        fun BufferedWriter.writeLn(line: String) {
            this.write(line)
            this.newLine()
        }
    }
}