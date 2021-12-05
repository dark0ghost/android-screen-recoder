package org.dark0ghost.android_screen_recorder.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import org.dark0ghost.android_screen_recorder.MainActivity.Companion.writeLn
import org.dark0ghost.android_screen_recorder.interfaces.GetIntent
import org.dark0ghost.android_screen_recorder.interfaces.GetsDirectory
import org.dark0ghost.android_screen_recorder.listeners.RListener
import org.dark0ghost.android_screen_recorder.states.BaseState
import org.dark0ghost.android_screen_recorder.time.CustomSubtitlesTimer
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.utils.setUiState
import org.vosk.Recognizer
import org.vosk.android.SpeechStreamService
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SpeechService: GetsDirectory, Service() {

    private val rListener: RListener = RListener
        .Builder()
        .setCallbackOnFinalResult {
            setUiState(BaseState.DONE)
            val file = createSubtitleFileOrDefault()
            Log.d("File/OnFinalResult", file.absoluteFile.toString())
            cleanSubtitleFile()
            buffer.clear()
            subtitlesCounter = 0
            timer.stop()
            oldTime = "00:00:00"
        }
        .setCallbackOnTimeout {
            setUiState(BaseState.DONE)
            if (speechStreamService != null) {
                speechStreamService = null
            }
        }
        .setCallbackOnResult { it ->
            val template = """
            $subtitlesCounter
            $oldTime-->${timer.nowTime}    
            $it\n   
            """.trimIndent()
            this@setCallbackOnResult.buffer.add(template)
            val file = createSubtitleFileOrDefault()
            file.bufferedWriter().writeLn(template)
            Log.d("File/OnResult", template)
            subtitlesCounter++
            oldTime = timer.nowTime
        }
        .build()

    private val binder =  SpeechBinder()
    private val exceptionForResultFile = Exception("file not created")
    private val timer: CustomSubtitlesTimer = CustomSubtitlesTimer()

    private var speechService: org.vosk.android.SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

    private var subtitlesCounter: Long = 1L
    private var subtitleResult: Result<File> = Result.failure(exceptionForResultFile)
    private var oldTime: String = "00:00:00"

    private lateinit var model: org.vosk.Model

    private fun cleanSubtitleFile(){
        subtitleResult = Result.failure(exceptionForResultFile)
    }

    private fun createSubtitleFileOrDefault(): File {
        val textFile = File(
            getsDirectory(),
            "${
                SimpleDateFormat(
                    Settings.MainActivitySettings.FILE_NAME_FORMAT,
                    Locale.US
                ).format(System.currentTimeMillis())
            }.srt"
        )
        if (subtitleResult.isSuccess) {
            return subtitleResult.getOrDefault(textFile)
        }
        subtitleResult = Result.success(textFile)
        return textFile
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
        }
    }

    fun stop(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stopMicrophone()
        }
    }

    fun close(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stopMicrophone()
        }
        speechStreamService = null
        speechService?.apply {
            stop()
            shutdown()
        }
    }

    // GetsDirectory

    override fun getsDirectory(): String {
        val rootDir = "${getExternalFilesDir("media")!!.absolutePath}/${Settings.MediaRecordSettings.NAME_DIR_SUBTITLE}/"
        val file = File(rootDir)
        if (!file.exists()) {
            Log.e(
                "getsDirectory/mkdirs", if (file.mkdirs()) {
                    "path is created"
                } else {
                    "path isn't create"
                }
            )
        }
        Log.i("getsDirectory", "${this::class.simpleName}: $rootDir")
        return rootDir
    }

    // End GetsDirectory

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * [android.os.IBinder] is usually for a complex interface
     * that has been [described using
 * aidl]({@docRoot}guide/components/aidl.html).
     *
     *
     * *Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process*.  More information about the main thread can be found in
     * [Processes and
 * Threads]({@docRoot}guide/topics/fundamentals/processes-and-threads.html).
     *
     * @param intent The Intent that was used to bind to this service,
     * as given to [ Context.bindService][android.content.Context.bindService].  Note that any extras that were included with
     * the Intent at that point will *not* be seen here.
     *
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    override fun onBind(intent: Intent?): IBinder = binder

    inner class SpeechBinder : Binder() {
        internal val service: SpeechService
            get() = this@SpeechService
    }


    companion object : GetIntent {
        override fun intent(context: Context): Intent = Intent(context, SpeechService::class.java)
    }
}