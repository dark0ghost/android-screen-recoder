package org.dark0ghost.android_screen_recorder

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
            recordService?.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
            Log.e("onServiceConnected", "init recordService${recordService.hashCode()}")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // There are no request codes
            result.data?.let { data ->
                if (result.resultCode == Activity.RESULT_OK) {
                    mediaProjectionMain =
                        projectionManager.getMediaProjection(result.resultCode, data)
                    recordService?.apply {
                        this@apply.mediaProjection = mediaProjectionMain
                        startRecord()
                    }
                }
            }
        }

    private val resultButtonLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.let { data ->
                if (result.resultCode == Activity.RESULT_OK) {
                    mediaProjectionMain =
                        projectionManager.getMediaProjection(result.resultCode, data)
                    recordService?.apply {
                        this@apply.mediaProjection = mediaProjectionMain
                        startRecord()
                    }
                }
            }
        }


    private lateinit var model: org.vosk.Model
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var mediaProjectionMain: MediaProjection
    private lateinit var startRecorder: Button

    private var recordService: RecordService? = null
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

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
        val permissionCheck =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return
        }
        initModel()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        setUiState(BaseState.START)

        startRecorder = findViewById(R.id.start_record)
        startRecorder.setOnClickListener {
            recordService?.apply {
                if (running) {
                    stopRecord()
                    return@setOnClickListener
                }
                val captureIntent = projectionManager.createScreenCaptureIntent()
                Log.d("start captureIntent", resultButtonLauncher.hashCode().toString())
                resultButtonLauncher.launch(captureIntent)
                return@setOnClickListener
            }
            Log.e("setOnClickListener", "recordService is null")
        }

        LibVosk.setLogLevel(LogLevel.INFO)

        checkPermissionsOrInitialize()

        resultLauncher.launch(intent)

        val intentService = Intent(this, RecordService::class.java)
        bindService(intentService, connection, BIND_AUTO_CREATE)
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