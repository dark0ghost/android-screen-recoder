package org.dark0ghost.android_screen_recorder.shortcuts

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import org.dark0ghost.android_screen_recorder.base.BaseRecordable
import org.dark0ghost.android_screen_recorder.services.ButtonService
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.callbackForStartRecord
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.isStartButton

class ComposeActivity: BaseRecordable() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("$this: onCreate","start shortcut")
        initService()
        if(!isStartButton) {
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
        Toast.makeText(
            this,
            "button now started", Toast.LENGTH_SHORT
        ).show()
        listRecordable = listOf(speechController, serviceController)
        callbackForStartRecord = callback@{
            clickButton()
            return@callback isStartRecord
        }
    }
}