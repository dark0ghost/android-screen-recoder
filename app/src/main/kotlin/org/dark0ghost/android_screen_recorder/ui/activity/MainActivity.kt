package org.dark0ghost.android_screen_recorder.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dark0ghost.android_screen_recorder.R
import org.dark0ghost.android_screen_recorder.base.AbstractBaseRecordable
import org.dark0ghost.android_screen_recorder.controllers.RecordController
import org.dark0ghost.android_screen_recorder.controllers.SpeechController
import org.dark0ghost.android_screen_recorder.interfaces.GetsDirectory
import org.dark0ghost.android_screen_recorder.interfaces.Recordable
import org.dark0ghost.android_screen_recorder.services.ButtonService
import org.dark0ghost.android_screen_recorder.states.ClickState
import org.dark0ghost.android_screen_recorder.ui.composable.MainUI
import org.dark0ghost.android_screen_recorder.utils.Settings.AudioRecordSettings.PERMISSIONS_REQUEST_RECORD_AUDIO
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.callbackForStartRecord
import org.dark0ghost.android_screen_recorder.utils.Settings.InlineButtonSettings.isStartButton
import org.dark0ghost.android_screen_recorder.utils.Settings.MediaRecordSettings.NAME_DIR_SUBTITLE
import org.dark0ghost.android_screen_recorder.utils.Settings.Model.model
import org.dark0ghost.android_screen_recorder.utils.getScreenCaptureIntent
import org.dark0ghost.android_screen_recorder.utils.isPermissionsGranted
import org.dark0ghost.android_screen_recorder.utils.startRecordable
import org.dark0ghost.android_screen_recorder.utils.stopRecordable
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException


class MainActivity : GetsDirectory, AbstractBaseRecordable() {

    private lateinit var intentButtonService: Intent

    private fun initModel() {
        val callbackModelInit = { models: org.vosk.Model ->
            model = models
        }
        StorageService.unpack(
            this, "model_ru", "models", callbackModelInit
        ) { exception: IOException ->
            Log.e("init-model-fn", "Failed to unpack the model ${exception.printStackTrace()}")
        }
        Log.d("initModel", "run complete")
    }

    private fun requestPermission() {
        var arrayPermission = arrayOf(
            Manifest.permission.RECORD_AUDIO,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            arrayPermission = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE
            )
        }

        ActivityCompat.requestPermissions(
            this,
            arrayPermission,
            PERMISSIONS_REQUEST_RECORD_AUDIO
        )
    }

    private fun checkPermissionsOrInitialize() {
        val permissionCheckRecordAudio =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        var arrayPermission = arrayOf(
            Manifest.permission.RECORD_AUDIO,
        )
        var checkPermission = permissionCheckRecordAudio != PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val permissionCheckForegroundService =
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            arrayPermission = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE
            )
            checkPermission =
                permissionCheckRecordAudio != PackageManager.PERMISSION_GRANTED || permissionCheckForegroundService != PackageManager.PERMISSION_GRANTED
        }
        if (checkPermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayPermission,
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return
        }
        initModel()
    }

    private fun inlineButton() {
        Log.d(
            "buttonStartInlineButton", if (!isStartButton) {
                "build button"
            } else {
                "deleted button"
            }
        )
        if (!isStartButton) {
            isStartButton = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !Settings.canDrawOverlays(
                    this
                )
            ) {
                intentButtonService = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intentButtonService)
                return
            }
            intentButtonService = ButtonService.intent(this)
            startService(intentButtonService)
            return
        }
        try {
            stopService(intentButtonService)
        } catch (_: java.lang.IllegalArgumentException){

        }
        isStartButton = true
        return
    }


    // GetsDirectory

    override fun getsDirectory(): String {
        val rootDir = "${getExternalFilesDir("media")!!.absolutePath}/${NAME_DIR_SUBTITLE}/"
        val file = File(rootDir)
        if (!file.exists()) {
            Log.e(
                "getsDirectory/mkdirs", if (file.mkdirs()) {
                    "path is created"
                } else {
                    "path isn't create"
                }
            )
        }
        Log.i("getsDirectory", "${this::class.simpleName}: $rootDir")
        return rootDir
    }

    // End GetsDirectory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission()
        setContent {
            MainUI {
                inlineButton()
                clickButton()
            }
        }
        supportActionBar?.hide() ?: Log.e("onCreate", "supportActionBar is null")

        intentButtonService = ButtonService.intent(this)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        initService()

        callbackForStartRecord = {
            clickButton()
            isStartRecord
        }

       checkPermissionsOrInitialize()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceController.close()
        stopService(intentButtonService)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initModel()
                return
            }
            finish()
        }
    }
}
