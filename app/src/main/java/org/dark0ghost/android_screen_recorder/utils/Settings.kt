package org.dark0ghost.android_screen_recorder.utils

import android.graphics.Color
import android.util.Log

object Settings {
   object AudioRecordSettings {
      const val PERMISSIONS_REQUEST_RECORD_AUDIO: Int = 1
   }

   object MediaRecordSettings {
      const val BIT_RATE: Int = 5 * 1024 * 1024
      const val VIDEO_FRAME_RATE: Int = 60
      const val NAME_DIR: String = "ScreenRecord"
      const val SERVICE_THREAD_NAME: String = "service_thread"
      const val WIDTH: Int = 1920
      const val HEIGHT: Int = 1080
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
      const val START_COLOR: Int = Color.RED
      const val STOP_COLOR: Int = Color.BLUE
      var callbackForStartRecord: () -> Unit = { Log.e("InlineButtonSettings", "fn not init") }
   }

   object MainActivitySettings {
      const val FILE_NAME_FORMAT: String = "yyyy-MM-dd-HH-mm-ss-SSS"
   }
}