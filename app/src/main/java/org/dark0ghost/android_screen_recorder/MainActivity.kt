package org.dark0ghost.android_screen_recorder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private fun initModel() {

    }

    private fun startRecordingVoice() {

    }

    private fun startRecordingScreen() {

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LibVosk.setLogLevel(LogLevel.INFO)

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}