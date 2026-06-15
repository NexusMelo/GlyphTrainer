package com.example.glyphtrainer

object AppMode {

    enum class Mode {
        PLAY,
        PROGRAM
    }

    var currentMode: Mode = Mode.PLAY
}