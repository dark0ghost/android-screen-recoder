package org.dark0ghost.android_screen_recorder.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.PixelFormat
import android.os.*
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import org.dark0ghost.android_screen_recorder.R
import org.dark0ghost.android_screen_recorder.interfaces.GetIntent
import org.dark0ghost.android_screen_recorder.states.ClickState
import org.dark0ghost.android_screen_recorder.ui.composable.RevoltUi
import org.dark0ghost.android_screen_recorder.utils.Settings
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.callbackForStartRecord


class ButtonService: Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var composeView: ComposeView
    private lateinit var buttonStartRecorder: ImageButton

    private class ButtonServiceLifecycleOwner: SavedStateRegistryOwner {
        private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
        private var mSavedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.create(this)

        @Deprecated("android 25", ReplaceWith("true"))
        val isInitialized: Boolean
            get() = true

        override fun getLifecycle(): Lifecycle {
            return mLifecycleRegistry
        }

        @Deprecated("android 25")
        fun setCurrentState(state: Lifecycle.State) {
            mLifecycleRegistry.currentState = state
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            mLifecycleRegistry.handleLifecycleEvent(event)
        }

        override fun getSavedStateRegistry(): SavedStateRegistry {
            return mSavedStateRegistryController.savedStateRegistry
        }

        fun performRestore(savedState: Bundle?) {
            mSavedStateRegistryController.performRestore(savedState)
        }

        @Deprecated("android 25")
        fun performSave(outBundle: Bundle) {
            mSavedStateRegistryController.performSave(outBundle)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        composeView = ComposeView(this).apply {

            setContent {
                RevoltUi {
                    stopService(intent(this@ButtonService))
                }
            }
        }

        val lifecycleOwner = ButtonServiceLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        ViewTreeLifecycleOwner.set(composeView, lifecycleOwner)
        ViewTreeSavedStateRegistryOwner.set(composeView, lifecycleOwner)

        val viewModelStore = ViewModelStore()
        ViewTreeViewModelStoreOwner.set(composeView) { viewModelStore }

        params =
            WindowManager.LayoutParams(
                Settings.InlineButtonSettings.WIDTH,
                Settings.InlineButtonSettings.HEIGHT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.RGBA_F16
            )
        params.x = 10
        params.y = 500 // service position
        params.gravity = Gravity.START or Gravity.TOP
        windowManager.addView(composeView, params)
        composeView.setOnTouchListener(object : View.OnTouchListener {
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
                        windowManager.updateViewLayout(composeView, paramsF)
                    }

                }
                return false
            }
        }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(composeView)
    }

    companion object : GetIntent {
        override fun intent(context: Context): Intent =
            Intent(context, ButtonService::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            }
    }
}