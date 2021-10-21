package org.dark0ghost.android_screen_recorder.utils

import android.util.Log
import org.dark0ghost.android_screen_recorder.state.BaseState
import java.lang.Exception
import org.vosk.android.RecognitionListener as RListener

class CustomRecognitionListener: RListener {

    private val buffer: MutableList<String> = mutableListOf()

    fun buffer() = buffer

    fun clearBuffer() = buffer.clear()

    override fun onPartialResult(p0: String) {
        buffer.add(p0)
    }

    override fun onResult(p0: String) {
        buffer.add(p0)
    }

    override fun onFinalResult(p0: String) {
        TODO("Not yet implemented")
    }

    override fun onError(p0: Exception) {
        Log.e(this::class.toString(), p0.message.orEmpty())
    }

    override fun onTimeout() {
        setUiState(BaseState.DONE)
    }
}