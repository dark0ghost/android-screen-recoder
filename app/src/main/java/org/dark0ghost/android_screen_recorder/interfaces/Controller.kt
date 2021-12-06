package org.dark0ghost.android_screen_recorder.interfaces

interface Controller: Recordable {
    val connected: Boolean

    fun stopService(): Boolean

    suspend fun startService(): Boolean
}