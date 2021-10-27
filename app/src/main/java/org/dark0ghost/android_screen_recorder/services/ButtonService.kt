package org.dark0ghost.android_screen_recorder.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.RelativeLayout
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.R

class ButtonService: Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var topView: RelativeLayout
    private lateinit var button: Button

    override fun onCreate() {
        super.onCreate();
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        topView = LayoutInflater.from(this).inflate(R.layout.revolt, null) as RelativeLayout
        topView.visibility = View.VISIBLE
        button = topView.findViewById(R.id.grub)
        params = WindowManager.LayoutParams(
            Settings.InlineButtonSettings.WIDTH,
            Settings.InlineButtonSettings.HEIGHT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.RGBA_F16
        )
        params.x = 10
        params.y = 500 // service position
        params.gravity = Gravity.TOP or Gravity.END
        windowManager.addView(topView, params)

        button.setOnClickListener { _ ->
            println("click")
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(topView)
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, Button::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
}