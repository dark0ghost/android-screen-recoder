package org.dark0ghost.android_screen_recorder

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
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
import org.dark0ghost.android_screen_recorder.services.ButtonService
import org.dark0ghost.android_screen_recorder.services.RecordService
import org.dark0ghost.android_screen_recorder.services.RecordService.RecordBinder
import org.dark0ghost.android_screen_recorder.states.BaseState
import org.dark0ghost.android_screen_recorder.utils.Settings.AudioRecordSettings.PERMISSIONS_REQUEST_RECORD_AUDIO
import org.dark0ghost.android_screen_recorder.utils.setUiState
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException
import java.io.InputStream
import org.vosk.android.RecognitionListener as RListener


class MainActivity : AppCompatActivity(), RListener {

    private val buffer: MutableList<String> = mutableListOf()
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

    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

    private var mBound: Boolean = false

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

    private fun recognizeFile() {
        if (speechStreamService != null) {
            setUiState(BaseState.DONE)
            speechStreamService!!.stop()
            speechStreamService = null
        } else {
            setUiState(BaseState.FILE)
            try {
                val rec = Recognizer(
                    model, 16000f, "[\"one zero zero zero one\", " +
                            "\"oh zero one two three four five six seven eight nine\", \"[unk]\"]"
                )
                val ais: InputStream = assets.open(
                    "10001-90210-01803.wav"
                )
                if (ais.skip(44) != 44L) throw IOException("File too short")
                speechStreamService = SpeechStreamService(rec, ais, 16000F)
                speechStreamService!!.start(this)
            } catch (e: IOException) {
                e.message?.let { Log.e("recognizeFile", it) }
            }
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
            speechService?.apply {
                startListening(this@MainActivity)
            }
        } catch (e: IOException) {
            e.message?.let { Log.e("recognizeFile", it) }
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        setUiState(BaseState.START)

        buttonService = ButtonService()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }else{
            startActivity(ButtonService.intent(this))
        }

        startRecorder = findViewById(R.id.start_record)
        startRecorder.setOnClickListener {
            try {
                recordService.apply {
                    if (running) {
                        stopRecord()
                        return@setOnClickListener
                    }
                    val captureIntent = projectionManager.createScreenCaptureIntent()
                    Log.d("start captureIntent", resultButtonLauncher.hashCode().toString())
                    resultButtonLauncher.launch(captureIntent)
                    return@setOnClickListener
                }
            } catch (e: java.lang.Exception) {
                Log.e("setOnClickListener", "recordService: $e")
            }
        }

        LibVosk.setLogLevel(LogLevel.INFO)

        checkPermissionsOrInitialize()

        resultLauncher.launch(intent)
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

    // RListener

    override fun onPartialResult(p0: String) {
        buffer.add(p0)
    }

    override fun onResult(p0: String) {
        buffer.add(p0)
    }

    override fun onFinalResult(p0: String) {
        buffer.add(p0)
        setUiState(BaseState.DONE);
        if (speechStreamService != null) {
            speechStreamService = null
        }
    }

    override fun onError(p0: Exception) {
        Log.e(this::class.toString(), p0.message.orEmpty())
    }

    override fun onTimeout() {
        setUiState(BaseState.DONE)
    }
}