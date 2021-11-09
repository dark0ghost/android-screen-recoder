package org.dark0ghost.android_screen_recorder.time

import org.dark0ghost.android_screen_recorder.utils.Settings.CustomSubtitlesTimerSettings.SUBTITLES_FORMAT_PATTERN
import java.text.SimpleDateFormat
import java.util.*

class CustomSubtitlesTimer {
    private val dateFormat: SimpleDateFormat = SimpleDateFormat(SUBTITLES_FORMAT_PATTERN, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private var startTime: Long = System.currentTimeMillis()

    var isRun: Boolean = false
        private set

    val nowTime: String
        get() =
            dateFormat.format(System.currentTimeMillis() - startTime)


    fun start() {
        if (!isRun) {
            startTime = System.currentTimeMillis()
            isRun = true
        }
    }

    fun stop() {
        isRun = false
    }
}