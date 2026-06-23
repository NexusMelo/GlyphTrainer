package pt.vicktor.glyphon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppMode.currentMode = AppMode.Mode.PLAY

        // abre o sistema atual
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
