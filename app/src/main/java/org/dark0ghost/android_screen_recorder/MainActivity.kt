package org.dark0ghost.android_screen_recorder

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ColorSpace.Model
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.dark0ghost.android_screen_recorder.utils.Settings.PERMISSIONS_REQUEST_RECORD_AUDIO
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService


class MainActivity : AppCompatActivity() {

    private val model: Model? = null
    private val speechService: SpeechService? = null
    private val speechStreamService: SpeechStreamService? = null
    private val resultView: TextView? = null

    private fun initModel() {

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
        if (speechService != null) {
            speechService.setPause(checked)
        }
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