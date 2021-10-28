package org.dark0ghost.android_screen_recorder.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.RelativeLayout
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.R
import org.dark0ghost.android_screen_recorder.interfaces.GetIntent
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.START_COLOR
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.STOP_COLOR
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.callbackForStartRecord

class ButtonService: Service() {
    private var colorBound = true

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var topView: RelativeLayout
    private lateinit var buttonStartRecorder: Button


    override fun onCreate() {
        super.onCreate();
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val nullParent: ViewGroup? = null
        topView = LayoutInflater.from(this).inflate(R.layout.revolt, nullParent) as RelativeLayout
        topView.visibility = View.VISIBLE
        buttonStartRecorder = topView.findViewById(R.id.grub)
        params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                Settings.InlineButtonSettings.WIDTH,
                Settings.InlineButtonSettings.HEIGHT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.RGBA_F16
            )
        } else {
            WindowManager.LayoutParams(
                Settings.InlineButtonSettings.WIDTH,
                Settings.InlineButtonSettings.HEIGHT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            )
        }
        params.x = 10
        params.y = 500 // service position
        params.gravity = Gravity.TOP or Gravity.END
        windowManager.addView(topView, params)

        buttonStartRecorder.setOnClickListener { _ ->
            Log.i("buttonStartRecorder", "callback is start")
            callbackForStartRecord()
            if (colorBound) {
                buttonStartRecorder.setBackgroundColor(START_COLOR)
                colorBound = false
                Log.i("buttonStartRecorder", "start recorder")
                return@setOnClickListener
            }
            buttonStartRecorder.setBackgroundColor(STOP_COLOR)
            colorBound = true
            Log.i("buttonStartRecorder", "stop recorder")
            return@setOnClickListener
        }

    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(topView)
    }

    companion object : GetIntent {
        override fun intent(context: Context): Intent =
            Intent(context, ButtonService::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
    }
}