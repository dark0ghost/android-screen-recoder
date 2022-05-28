package org.dark0ghost.android_screen_recorder.utils

import android.app.Activity
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.util.Log
import org.dark0ghost.android_screen_recorder.interfaces.Recordable
import org.dark0ghost.android_screen_recorder.states.BaseState

fun closeServiceNotification(context: Context, id: Int) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(id)
}

fun isPermissionsGranted(permissions: Map<String, Boolean>): Boolean {
    var granted = true
    for (permission in permissions) {
        granted = granted  && permission.value
        if (!granted) break
    }
    return granted
}

fun getScreenCaptureIntent(context: Context): Intent {
    val projectionManager = context.getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    return projectionManager.createScreenCaptureIntent()
}

fun isPermissionGranted(activity: Activity, permission: String): Boolean {
    val result = activity.checkSelfPermission(permission)
    return result == PackageManager.PERMISSION_GRANTED
}

fun isPermissionsGranted(activity: Activity, permissions: Array<String>): Boolean {
    var permissionsGranted = true
    for (permission in permissions) {
        permissionsGranted =  isPermissionGranted(activity, permission)
        Log.d("sPermissionsGranted", "permissionsGranted: $permissionsGranted")
        if (!permissionsGranted) {
            break
        }
    }
    return permissionsGranted
}

fun<T: Recordable> startRecordable(recordable: T){
    recordable.startRecording()
}

fun<T: Recordable> stopRecordable(recordable: T){
    recordable.stopRecording()
}