package org.dark0ghost.android_screen_recorder.listeners

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.dark0ghost.android_screen_recorder.data_class.TextFromVoice
import org.vosk.android.RecognitionListener

class RListener(private var callbackOnTimeout: RListener.() -> Unit, private var callbackOnResult: RListener.(String) -> Unit, private var callbackOnFinalResult: RListener.() -> Unit): RecognitionListener {

    val buffer: MutableList<String> = mutableListOf()

    override fun onPartialResult(p0: String) {
        val obj = Json.decodeFromString<TextFromVoice>(p0)
        Log.e("word/onPartialResult", obj.toString())
    }

    override fun onResult(p0: String) {
        val obj = Json.decodeFromString<TextFromVoice>(p0)
        callbackOnResult(obj.text)
        Log.e("word/onResult", obj.toString())
    }

    override fun onFinalResult(p0: String) {
        val obj = Json.decodeFromString<TextFromVoice>(p0)
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
        private var callbackOnTimeout: RListener.() -> Unit = {},
        private var callbackOnFinalResult: RListener.() -> Unit = {},
        private var callbackOnResult: RListener.(String) -> Unit = {}
    ) {
        fun setCallbackOnTimeout(callback: RListener.() -> Unit) = apply {
            callbackOnTimeout = callback
        }

        fun setCallbackOnFinalResult(callback: RListener.() -> Unit) = apply {
            callbackOnFinalResult = callback
        }

        fun setCallbackOnResult(callback: RListener.(String) -> Unit) = apply {
            callbackOnResult = callback
        }

        fun build(): RListener {
            return RListener(callbackOnTimeout, callbackOnResult, callbackOnFinalResult)
        }
    }
}