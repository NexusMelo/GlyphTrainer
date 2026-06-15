package com.example.glyphtrainer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // ← obrigatório para Theme.SplashScreen

        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        val jogarBtn = Button(this).apply {
            setText(R.string.action_play)
            textSize = 22f
            setOnClickListener {
                startActivity(Intent(this@MenuActivity, GameActivity::class.java))
            }
        }

        val programarBtn = Button(this).apply {
            setText(R.string.action_program)
            textSize = 22f
            visibility = View.GONE
            setOnClickListener {
                startActivity(Intent(this@MenuActivity, PasswordActivity::class.java))
            }
        }

        layout.addView(jogarBtn)
        layout.addView(programarBtn)

        setContentView(layout)
    }
}
