package org.dark0ghost.android_screen_recorder.utils

import android.graphics.Color
import android.util.Log

object Settings {
   object AudioRecordSettings {
      const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
   }

   object MediaRecordSettings {
      const val BIT_RATE = 5 * 1024 * 1024
      const val VIDEO_FRAME_RATE = 60
      const val NAME_DIR = "ScreenRecord"
      const val SERVICE_THREAD_NAME = "service_thread"
      const val WIDTH = 1920
      const val HEIGHT = 1080
   }

   object NotificationSettings {
      const val CHANNEL_ID = "recorder"
      const val CONTENT_TITTLE = "DataRecorder"
      const val CONTENT_TEXT = "Your screen is being recorded and saved to your phone."
      const val FOREGROUND_ID = 1
   }

   object InlineButtonSettings {
      const val WIDTH = 400
      const val HEIGHT = 400
      const val START_COLOR = Color.RED
      const val STOP_COLOR  = Color.BLUE
      var callbackForStartRecord: () -> Unit = { Log.e("InlineButtonSettings", "fn not init") }
   }
}