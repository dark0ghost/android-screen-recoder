package org.dark0ghost.android_screen_recorder.dagger2.model

import android.app.Service
import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import org.dark0ghost.android_screen_recorder.manager.SpeechManager
import org.dark0ghost.android_screen_recorder.services.SpeechService
import org.dark0ghost.android_screen_recorder.states.BaseState
import org.dark0ghost.android_screen_recorder.utils.setUiState
import org.vosk.Model
import org.vosk.android.StorageService
import java.io.IOException
import javax.inject.Singleton


@Module
class VoiceModule {

    @Singleton
    @Provides
    fun provideSpeechManager(model: Model, context: Context): SpeechManager =
        SpeechManager(context = context, model = model)

    @Singleton
    @Provides
    fun provideSpeechService(): Service =
        SpeechService()


    @Singleton
    @Provides
    fun provideModel(context: Context): Model {
        var model = Model()
        val callbackModelInit = { models: Model ->
            model = models
            setUiState(BaseState.READY)
        }
        StorageService.unpack(
            context, "model_ru", "models", callbackModelInit
        ) { exception: IOException ->
            Log.e("init-model-fn", "Failed to unpack the model ${exception.printStackTrace()}")
        }
        Log.d("initModel", "run complete")
        return model
    }

}