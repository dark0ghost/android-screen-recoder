package org.dark0ghost.android_screen_recorder.manager

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.dark0ghost.android_screen_recorder.data_class.FileBuffer
import org.dark0ghost.android_screen_recorder.listeners.RListener
import org.dark0ghost.android_screen_recorder.states.BaseState
import org.dark0ghost.android_screen_recorder.time.CustomSubtitlesTimer
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.utils.Settings.MainActivitySettings.FILE_NAME_FORMAT
import org.dark0ghost.android_screen_recorder.utils.setUiState
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechStreamService
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class SpeechManager(private val model: Model) {

    private val rListener: RListener = RListener
        .Builder()
        .setCallbackOnFinalResult {
            setUiState(BaseState.DONE)
            val fileData = createSubtitleFileDataOrDefault()
            Log.d("File/OnFinalResult", fileData.file.length().toString())
            Log.d("File/OnFinalResult", "buffer size: ${buffer.size}")
            Log.d("File/OnFinalResult", "path: ${fileData.file.absolutePath}")
            if (fileData.file.length() == 0L) {
                val bufferedWriter = fileData.bufferedWriter
                bufferedWriter.use {
                    buffer.forEach { i ->
                        it.append(i)
                    }
                    it.close()
                }
                Log.d("File/OnFinalResult", fileData.file.length().toString())
            }
            cleanSubtitleFile()
            buffer.clear()
            subtitlesCounter = 1
            timer.stop()
            oldTime = "00:00:00,000"
        }
        .setCallbackOnTimeout {
            setUiState(BaseState.DONE)
            speechStreamService?.let {
                speechService = null
            }
        }
        .setCallbackOnResult { it ->
            val template = "$subtitlesCounter\n$oldTime --> ${timer.nowTime}\n$it\n"
            this@setCallbackOnResult.buffer.add(template)
            val fileData = createSubtitleFileDataOrDefault()
            try {
                fileData.bufferedWriter.append(template)
                fileData.bufferedWriter.close()
            } catch (e: IOException) {
                cleanSubtitleFile()
                val fileBuffer = createSubtitleFileDataOrDefault()
                fileBuffer.bufferedWriter.use {
                    it.append(template)
                }
                e.printStackTrace()
            }
            Log.d("File/OnResult", "file size: ${fileData.file.length()}")
            Log.d("File/OnResult", "file path: ${fileData.file.absolutePath}")
            Log.d("File/OnResult", template)
            subtitlesCounter++
            oldTime = timer.nowTime
        }
        .build()

    private val exceptionForResultFile = Exception("file not created")
    private val timer: CustomSubtitlesTimer = CustomSubtitlesTimer()

    private var speechService: org.vosk.android.SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

    private var subtitlesCounter: Long = 1L
    private var subtitleResult: Result<String> = Result.failure(exceptionForResultFile)
    private var oldTime: String = "00:00:00,000"

    private fun cleanSubtitleFile() {
        subtitleResult = Result.failure(exceptionForResultFile)
    }

    private fun createSubtitleFileDataOrDefault(): FileBuffer {
        val directory = File("/storage/emulated/0/Documents/")
        Log.e("file create", (!directory.exists()).toString())
        if (!directory.exists()){
            directory.mkdirs()
        }
        val dataFormat = SimpleDateFormat(
            FILE_NAME_FORMAT,
            Locale.US
        ).format(System.currentTimeMillis())
        val textFile = File(
            directory,
            "$dataFormat.srt"
        )
        Log.d("FileDataOrDefault", subtitleResult.isSuccess.toString())
        Log.d("FileDataOrDefault", subtitleResult.getOrNull().toString())
        val writer = if (subtitleResult.isSuccess) {
            FileWriter(subtitleResult.getOrDefault(textFile.absolutePath), true)
        } else {
            subtitleResult = Result.success(textFile.absolutePath)
            try {
                FileWriter(textFile, true)
            } catch (e: FileNotFoundException) {
                Log.e("system", "test", e)
                FileWriter(textFile, true)
            }
        }
        val bufferedWriter = BufferedWriter(writer)
        val rFile = File(subtitleResult.getOrDefault(textFile.absolutePath))
        return FileBuffer(rFile, bufferedWriter)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun recognizeMicrophone() {
        setUiState(BaseState.MIC)
        try {
            val rec = Recognizer(model, Settings.AudioRecordSettings.SIMPLE_RATE)
            speechService =
                org.vosk.android.SpeechService(rec, Settings.AudioRecordSettings.SIMPLE_RATE)
            speechService?.startListening(rListener)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun stopMicrophone() {
        speechService?.let {
            setUiState(BaseState.DONE)
            it.stop()
            speechService = null
            return
        }
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recognizeMicrophone()
            timer.start()
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stopMicrophone()
        }
    }

    fun close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stopMicrophone()
        }
        speechStreamService = null
        speechService?.apply {
            stop()
            shutdown()
        }
    }
}