package org.dark0ghost.android_screen_recorder.utils

import android.media.MediaRecorder

object Settings {
   object AudioRecordSettings {
      const val PERMISSIONS_REQUEST_RECORD_AUDIO: Int = 1
      const val SIMPLE_RATE: Float = 16000.0f
   }

   object MediaRecordSettings {
      const val BIT_RATE: Int = 5 * 1024 * 1024
      const val VIDEO_FRAME_RATE: Int = 60
      const val NAME_DIR: String = "ScreenRecord"
      const val SERVICE_THREAD_NAME: String = "service_thread"
      const val WIDTH: Int = 1920
      const val HEIGHT: Int = 1080
      const val IGNORE_SIZE_DISPLAY: Boolean = true
      const val AUDIO_Encoder = MediaRecorder.AudioEncoder.AMR_NB
   }

   object NotificationSettings {
      const val CHANNEL_ID: String = "recorder"
      const val CONTENT_TITTLE: String = "DataRecorder"
      const val CONTENT_TEXT: String = "Your screen is being recorded and saved to your phone."
      const val FOREGROUND_ID: Int = 1
   }

   object InlineButtonSettings {
      const val WIDTH: Int = 400
      const val HEIGHT: Int = 400
      const val START_COLOR: Int = android.graphics.Color.RED
      const val STOP_COLOR: Int = android.graphics.Color.BLUE
      var callbackForStartRecord: () -> Unit =
         { android.util.Log.e("InlineButtonSettings", "fn not init") }
   }

   object MainActivitySettings {
      const val FILE_NAME_FORMAT: String = "yyyy-MM-dd-HH-mm-ss-SSS"
      const val HANDLER_DELAY: Long = 1000L
   }

   object CustomSubtitlesTimerSettings {
      const val SUBTITLES_FORMAT_PATTERN: String = "HH:mm:ss"
   }
}