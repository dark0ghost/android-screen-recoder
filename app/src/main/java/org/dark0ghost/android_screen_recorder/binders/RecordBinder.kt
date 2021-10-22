package org.dark0ghost.android_screen_recorder.binders

import android.os.Binder
import org.dark0ghost.android_screen_recorder.services.RecordService

internal class RecordBinder: Binder() {
    fun getRecordService(): RecordService {
        return RecordService()
    }
}
