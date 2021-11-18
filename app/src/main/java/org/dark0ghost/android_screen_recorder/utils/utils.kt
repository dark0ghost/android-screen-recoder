package org.dark0ghost.android_screen_recorder.utils

import android.app.NotificationManager
import android.content.Context
import org.dark0ghost.android_screen_recorder.states.BaseState

internal fun setUiState(state: BaseState) {
    when (state) {
        BaseState.START -> {

        }
        BaseState.READY -> {

        }
        BaseState.FILE -> {

        }
        BaseState.DONE -> {

        }
        BaseState.MIC -> {

        }
    }
}

fun closeServiceNotification(context: Context, id: Int) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(id)
}