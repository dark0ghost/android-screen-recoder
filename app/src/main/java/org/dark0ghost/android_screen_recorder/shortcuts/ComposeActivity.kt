package org.dark0ghost.android_screen_recorder.shortcuts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import org.dark0ghost.android_screen_recorder.services.ButtonService

class ComposeActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("$this: onCreate","start shortcut")
        val intentButtonService: Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !Settings.canDrawOverlays(
                this
            )
        ) {
            intentButtonService = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intentButtonService)
            finish()
            return
        }
        intentButtonService = ButtonService.intent(this)
        startService(intentButtonService)
        finish()
    }
}