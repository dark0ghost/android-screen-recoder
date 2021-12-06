package org.dark0ghost.android_screen_recorder.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import org.dark0ghost.android_screen_recorder.interfaces.GetIntent
import org.dark0ghost.android_screen_recorder.manager.SpeechManager

class SpeechService: Service() {
    private val binder: SpeechBinder = SpeechBinder()
    private val speechManager: SpeechManager = SpeechManager(this)

    fun start() {
        speechManager.start()
    }

    fun stop() {
        speechManager.stop()
    }

    fun close() {
        speechManager.close()
    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * [android.os.IBinder] is usually for a complex interface
     * that has been [described using
 * aidl]({@docRoot}guide/components/aidl.html).
     *
     *
     * *Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process*.  More information about the main thread can be found in
     * [Processes and
 * Threads]({@docRoot}guide/topics/fundamentals/processes-and-threads.html).
     *
     * @param intent The Intent that was used to bind to this service,
     * as given to [ Context.bindService][android.content.Context.bindService].  Note that any extras that were included with
     * the Intent at that point will *not* be seen here.
     *
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = if (intent.action != null) {
            intent.action
        } else {
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    inner class SpeechBinder : Binder() {
        internal val service: SpeechService
            get() = this@SpeechService
    }


    companion object : GetIntent {
        override fun intent(context: Context): Intent = Intent(context, SpeechService::class.java)
    }
}