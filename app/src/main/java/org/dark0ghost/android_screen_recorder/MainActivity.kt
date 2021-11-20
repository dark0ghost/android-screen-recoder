package org.dark0ghost.android_screen_recorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dark0ghost.android_screen_recorder.controllers.RecordController
import org.dark0ghost.android_screen_recorder.interfaces.GetsDirectory
import org.dark0ghost.android_screen_recorder.listeners.RListener
import org.dark0ghost.android_screen_recorder.services.ButtonService
import org.dark0ghost.android_screen_recorder.services.RecordService
import org.dark0ghost.android_screen_recorder.states.BaseState
import org.dark0ghost.android_screen_recorder.time.CustomSubtitlesTimer
import org.dark0ghost.android_screen_recorder.utils.Settings.AudioRecordSettings.PERMISSIONS_REQUEST_RECORD_AUDIO
import org.dark0ghost.android_screen_recorder.utils.Settings.AudioRecordSettings.SIMPLE_RATE
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.callbackForStartRecord
import org.dark0ghost.android_screen_recorder.utils.Settings.MainActivitySettings.FILE_NAME_FORMAT
import org.dark0ghost.android_screen_recorder.utils.Settings.MainActivitySettings.HANDLER_DELAY
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.COMMAND_START_SERVICE
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.EXTRA_COMMAND_KEY
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.NAME_DIR_SUBTITLE
import org.dark0ghost.android_screen_recorder.utils.Settings.PermissionsSettings.READ_WRITE_PERMISSIONS
import org.dark0ghost.android_screen_recorder.utils.Settings.PermissionsSettings.RECORD_AUDIO_PERMISSIONS
import org.dark0ghost.android_screen_recorder.utils.getScreenCaptureIntent
import org.dark0ghost.android_screen_recorder.utils.isPermissionsGranted
import org.dark0ghost.android_screen_recorder.utils.setUiState
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : GetsDirectory, AppCompatActivity() {
    private val rListener: RListener = RListener
        .Builder()
        .setCallbackOnFinalResult {
            setUiState(BaseState.DONE)
            val textFile = File(
                getsDirectory(),
                "${
                    SimpleDateFormat(
                        FILE_NAME_FORMAT,
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
            setUiState(BaseState.DONE)
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

        }
        .build()

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            startRecordInLauncher(result)
        }

    private val resultButtonLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            startRecordInLauncher(result)
        }

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

    private lateinit var model: org.vosk.Model
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var mediaProjectionMain: MediaProjection
    private lateinit var startRecorder: Button
    private lateinit var recordService: RecordService
    private lateinit var buttonStartInlineButton: Button
    private lateinit var intentButtonService: Intent
    private lateinit var serviceController: RecordController

    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

    private var boundInlineButton: Boolean = true
    private var subtitlesCounter: Long = 1L
    private var oldTime: String = "00:00:00"
    private val timer: CustomSubtitlesTimer = CustomSubtitlesTimer()

    private fun startRecordInLauncher(result: ActivityResult) {
        result.data?.let { data ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    recognizeMicrophone()
                }
                Handler(Looper.getMainLooper()).postDelayed({
                   val mediaProjectionMain =
                       projectionManager.getMediaProjection(result.resultCode, data)
                    serviceController.setupMediaProjection(mediaProjectionMain)
                    tryStartRecording()
                }, HANDLER_DELAY)
            }
        }
    }

    private fun initModel() {
        val callbackModelInit = { models: org.vosk.Model ->
            model = models
            setUiState(BaseState.READY)
        }
        StorageService.unpack(
            this@MainActivity, "model_ru", "models", callbackModelInit
        ) { exception: IOException ->
            Log.e("init-model-fn", "Failed to unpack the model ${exception.printStackTrace()}")
        }
        Log.d("initModel", "run complete")
    }

    private fun recognizeMicrophone() {
        setUiState(BaseState.MIC)
        try {
            val rec = Recognizer(model, SIMPLE_RATE)
            speechService = SpeechService(rec, SIMPLE_RATE)
            speechService?.startListening(rListener)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopMicrophone() {
        speechService?.let {
            setUiState(BaseState.DONE)
            it.stop()
            speechService = null
            return
        }
    }

    private fun initService() {
        serviceController = RecordController(this)
        lifecycleScope.launch {
            while(isActive && !serviceController.connected) {
                serviceController.startService()
            }
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

    private fun startRecord() {
        try {
            recordService.apply {
                if (running) {
                    Log.i("startRecord", "running is true")
                    stopMicrophone()
                    stopRecord()
                    return
                }
                val captureIntent = projectionManager.createScreenCaptureIntent().apply {
                    putExtra(
                        EXTRA_COMMAND_KEY,
                        COMMAND_START_SERVICE
                    )
                }
                Log.d("start captureIntent", resultButtonLauncher.hashCode().toString())
                resultButtonLauncher.launch(captureIntent)
                return
            }
        } catch (e: java.lang.Exception) {
            Log.e("startRecorder", "recordService: $e")
        }
    }

    private fun startRecording() {
        serviceController.startRecording()
    }

    private fun stopRecording() {
        serviceController.stopRecording()
    }

    private fun tryStartRecording() {
        lifecycleScope.launch {
            while (isActive && !serviceController.connected) {
                serviceController.startService()
                delay(1000)
            }
            if (!isActive) return@launch

            val permissions =
                RECORD_AUDIO_PERMISSIONS + READ_WRITE_PERMISSIONS
            val permissionsGranted = isPermissionsGranted(this@MainActivity, permissions)
            if (permissionsGranted && serviceController.isMediaProjectionConfigured) {
                startRecording()
            } else if (!permissionsGranted) {
                permissionsLauncher.launch(permissions)
            } else if (!serviceController.isMediaProjectionConfigured) {
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
        Log.e("getsDirectory", rootDir)
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
                "buttonStartInlineButton", if (boundInlineButton) {
                    "build button"
                } else {
                    "deleted button"
                }
            )
            if (boundInlineButton) {
                boundInlineButton = false
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
            boundInlineButton = true
            return@setOnClickListener
        }
        initService()

        callbackForStartRecord = { tryStartRecording() }

        startRecorder = findViewById(R.id.start_record)
        startRecorder.setOnClickListener {
            callbackForStartRecord()
        }

        LibVosk.setLogLevel(LogLevel.INFO)

        checkPermissionsOrInitialize()
        resultLauncher.launch(this@MainActivity.intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechService?.apply {
            stop()
            shutdown()
        }
        serviceController.close()
        speechStreamService?.stop()
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
}