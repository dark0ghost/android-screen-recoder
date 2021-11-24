package org.dark0ghost.android_screen_recorder.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.RelativeLayout
import org.dark0ghost.android_screen_recorder.R
import org.dark0ghost.android_screen_recorder.interfaces.GetIntent
import org.dark0ghost.android_screen_recorder.states.ClickState
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.START_COLOR
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.STOP_COLOR
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.callbackForStartRecord


class ButtonService: Service() {

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
        params.gravity = Gravity.START or Gravity.TOP
        windowManager.addView(topView, params)
        buttonStartRecorder.apply {
            setOnClickListener { _ ->
                Log.i("buttonStartRecorder", "callback is start")
                when (val state = callbackForStartRecord()) {
                    ClickState.IsClicked -> {
                        buttonStartRecorder.setBackgroundColor(START_COLOR)
                        Log.i("buttonStartRecorder", "start recorder")
                        return@setOnClickListener
                    }
                    ClickState.NotClicked -> {
                        buttonStartRecorder.setBackgroundColor(STOP_COLOR)
                        Log.i("buttonStartRecorder", "stop recorder")
                        return@setOnClickListener
                    }
                    else -> Log.e("clickButton", "isStartRecord have state:$state, this is ok?")
                }
            }

            setOnTouchListener(object : View.OnTouchListener {
                private val paramsF = params
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                /**
                 * Called when a touch event is dispatched to a view. This allows listeners to
                 * get a chance to respond before the target view.
                 *
                 * @param v The view the touch event has been dispatched to.
                 * @param event The MotionEvent object containing full information about
                 * the event.
                 * @return True if the listener has consumed the event, false otherwise.
                 */
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = paramsF.x
                            initialY = paramsF.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                        }

                        MotionEvent.ACTION_MOVE -> {
                            paramsF.x = initialX + (event.rawX - initialTouchX).toInt()
                            paramsF.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(topView, paramsF)
                        }

                    }
                    return false
                }
            }
            )
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