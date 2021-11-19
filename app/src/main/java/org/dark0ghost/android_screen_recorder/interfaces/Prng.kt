package org.dark0ghost.android_screen_recorder.interfaces

internal fun interface Prng {
    fun randomNumber(begin: Int, end: Int): Int
}