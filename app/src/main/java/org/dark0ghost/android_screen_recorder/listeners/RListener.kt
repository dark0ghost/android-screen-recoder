package org.dark0ghost.android_screen_recorder.listeners

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.dark0ghost.android_screen_recorder.data_class.TextFromVoice
import org.vosk.android.RecognitionListener

open class RListener(private var callbackOnTimeout: () -> Unit, private var callbackOnFinalResult: () -> Unit): RecognitionListener {

    open val buffer: MutableList<TextFromVoice> = mutableListOf()

    override fun onPartialResult(p0: String) {
        val obj = Json.decodeFromString<TextFromVoice>(p0)
        //buffer.add(obj)
        Log.e("word/onPartialResult", obj.toString())
    }

    override fun onResult(p0: String) {
        val obj = Json.decodeFromString<TextFromVoice>(p0)
        buffer.add(obj)

        Log.e("word/onResult", obj.toString())
    }

    override fun onFinalResult(p0: String) {
        val obj = Json.decodeFromString<TextFromVoice>(p0)
        buffer.add(obj)
        Log.e("word/onFinalResult", obj.toString())
        callbackOnFinalResult()
    }

    override fun onError(err: Exception) {
        err.printStackTrace()
    }

    override fun onTimeout() {
        callbackOnTimeout()
    }

    data class Builder(
        private var callbackOnTimeout: () -> Unit = {},
        private var callbackOnFinalResult: () -> Unit = {}
    ) {
        fun setCallbackOnTimeout(callback: () -> Unit) = apply {
            callbackOnTimeout = callback
        }

        fun setCallbackOnFinalResult(callback: () -> Unit) = apply {
            callbackOnFinalResult = callback
        }

        fun build(): RListener {
            return RListener(callbackOnTimeout, callbackOnFinalResult)
        }
    }
}