package org.dark0ghost.android_screen_recorder.dagger2.component

import android.app.Activity
import dagger.Component
import org.dark0ghost.android_screen_recorder.dagger2.model.VoiceModule


@Component(modules = [VoiceModule::class])
interface ApplicationComponent {
    fun inject(activity: Activity)
}