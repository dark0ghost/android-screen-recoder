package org.dark0ghost.android_screen_recorder

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.dark0ghost.android_screen_recorder.listeners.RListener
import org.dark0ghost.android_screen_recorder.services.ButtonService
import org.dark0ghost.android_screen_recorder.services.RecordService
import org.dark0ghost.android_screen_recorder.services.RecordService.RecordBinder
import org.dark0ghost.android_screen_recorder.states.BaseState
import org.dark0ghost.android_screen_recorder.utils.Settings.AudioRecordSettings.PERMISSIONS_REQUEST_RECORD_AUDIO
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.callbackForStartRecord
import org.dark0ghost.android_screen_recorder.utils.Settings.MainActivitySettings.FILE_NAME_FORMAT
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


class MainActivity : AppCompatActivity() {
    private val rListener: RListener = RListener.Builder()
        .setCallbackOnFinalResult {
            setUiState(BaseState.DONE)
            val textFile = File(
                getOutputDirectory(),
                SimpleDateFormat(
                    FILE_NAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".txt"
            )
            textFile.writeText(buffer.toString())
            buffer.clear()
        }
        .setCallbackOnTimeout {
            setUiState(BaseState.DONE)
            if (speechStreamService != null) {
                speechStreamService = null
            }
        }
        .build()
    private val connection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val metrics = resources.displayMetrics
            val binder = service as RecordBinder
            recordService = binder.getRecordService()
            recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
            Log.d("onServiceConnected", "init recordService{${recordService.hashCode()}}")
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            startRecordInLauncher(result)
        }

    private val resultButtonLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            startRecordInLauncher(result)
        }

    private lateinit var model: org.vosk.Model
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var mediaProjectionMain: MediaProjection
    private lateinit var startRecorder: Button
    private lateinit var recordService: RecordService
    private lateinit var buttonService: ButtonService
    private lateinit var buttonStartInlineButton: Button
    private lateinit var intentButtonService: Intent

    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

    private var mBound: Boolean = false
    private var boundInlineButton: Boolean = true

    private fun startRecordInLauncher(result: ActivityResult) {
        result.data?.let { data ->
            if (result.resultCode == Activity.RESULT_OK) {
                Handler(Looper.getMainLooper()).postDelayed({
                    mediaProjectionMain =
                        projectionManager.getMediaProjection(result.resultCode, data)
                    recordService.apply {
                        mediaProjection = mediaProjectionMain
                        startRecord()
                    }
                }, 1000)
            }
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, packageName.replace("org.dark0ghost.", "")).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun initModel() {
        val callbackModelInit = { models: org.vosk.Model ->
            model = models
            setUiState(BaseState.READY)
        }
        StorageService.unpack(
            this, "model_ru", "model", callbackModelInit
        ) { exception: IOException ->
            Log.e("init-model-fn", "Failed to unpack the model ${exception.message}")
        }
    }

    private fun recognizeMicrophone() {
        speechService?.let {
            setUiState(BaseState.DONE)
            it.stop()
            speechService = null
            return
        }
        setUiState(BaseState.MIC)
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(rListener)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun checkPermissionsOrInitialize() {
        val permissionCheckRecordAudio =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        var arrayPermission = arrayOf(
            Manifest.permission.RECORD_AUDIO,
        )
        var checkPermission = permissionCheckRecordAudio != PackageManager.PERMISSION_GRANTED
        Log.e(
            "check permission",
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P).toString()
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
            Log.e("check permission", checkPermission.toString())
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
            recognizeMicrophone()
            recordService.apply {
                if (running) {
                    Log.i("startRecord", "running is true")
                    stopRecord()
                    return
                }
                val captureIntent = projectionManager.createScreenCaptureIntent()
                Log.d("start captureIntent", resultButtonLauncher.hashCode().toString())
                resultButtonLauncher.launch(captureIntent)
                return
            }
        } catch (e: java.lang.Exception) {
            Log.e("startRecorder", "recordService: $e")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        intentButtonService = ButtonService.intent(this)
        setUiState(BaseState.START)

        buttonService = ButtonService()
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && !Settings.canDrawOverlays(
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
                startService(intentButtonService)
                boundInlineButton = false
                return@setOnClickListener
            }
            stopService(intentButtonService)
            boundInlineButton = true
            return@setOnClickListener
        }

        callbackForStartRecord = { startRecord() }

        startRecorder = findViewById(R.id.start_record)
        startRecorder.setOnClickListener {
            startRecord()
        }

        LibVosk.setLogLevel(LogLevel.INFO)

        checkPermissionsOrInitialize()

        resultLauncher.launch(this@MainActivity.intent)
    }

    override fun onStart() {
        super.onStart()
        // Bind to Service
        RecordService.intent(this).also {
            bindService(it, connection, BIND_AUTO_CREATE)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(it)
                return
            }
            startService(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        speechService?.apply {
            stop()
            shutdown()
        }
        speechStreamService?.stop()
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