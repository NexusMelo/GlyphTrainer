package pt.vicktor.glyphon

object AppMode {

    enum class Mode {
        PLAY,
        PROGRAM
    }

    var currentMode: Mode = Mode.PLAY
}
