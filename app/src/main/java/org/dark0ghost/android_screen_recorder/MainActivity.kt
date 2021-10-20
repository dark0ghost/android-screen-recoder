package org.dark0ghost.android_screen_recorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.dark0ghost.android_screen_recorder.state.BaseState
import org.dark0ghost.android_screen_recorder.utils.Settings.PERMISSIONS_REQUEST_RECORD_AUDIO
import org.dark0ghost.android_screen_recorder.utils.setUiState
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var model: org.vosk.Model
    private lateinit var speechService: SpeechService
    private lateinit var  speechStreamService: SpeechStreamService

    private fun initModel() {
        val callbackModelInit = { models: org.vosk.Model ->
            model = models
            setUiState(BaseState.READY)
        }
        StorageService.unpack(
            this, "model-en-us", "model", callbackModelInit
        ) { exception: IOException ->
            Log.e("init-model-fn","Failed to unpack the model ${exception.message}" )
        }
    }

    private fun startRecordingVoice() {

    }

    private fun startRecordingScreen() {

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
        }
        initModel()
    }

    private fun pause(checked: Boolean) {
        speechService.setPause(checked)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                initModel()
            } else {
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

}