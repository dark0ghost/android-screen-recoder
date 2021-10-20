package org.dark0ghost.android_screen_recorder.utils

import org.dark0ghost.android_screen_recorder.state.BaseState

fun setUiState(state: BaseState){
    when(state) {
        BaseState.START -> {

        }
        BaseState.READY -> {

        }
        BaseState.FILE -> {

        }
        BaseState.DONE -> {

        }
        BaseState.MIC -> {

        }
        else -> return
    }
}