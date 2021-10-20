package org.dark0ghost.android_screen_recorder.state

enum class BaseState(i: Int) {
    START(1),
    READY(1),
    DONE(2),
    FILE(3),
    MIC(4)
}