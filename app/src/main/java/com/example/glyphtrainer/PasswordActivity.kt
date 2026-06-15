package com.example.glyphtrainer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PasswordActivity : AppCompatActivity() {

    private val DEV_PASSWORD = "8032" // ← muda aqui quando quiseres

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.DKGRAY)
            setPadding(60,60,60,60)
        }

        val input = EditText(this).apply {
            setHint(R.string.pin_hint)
            textSize = 22f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val enterBtn = Button(this).apply {
            setText(R.string.action_enter)
            textSize = 20f

            setOnClickListener {
                if(input.text.toString() == DEV_PASSWORD){
                    startActivity(Intent(this@PasswordActivity, ProgramActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@PasswordActivity,
                        R.string.error_invalid_pin,
                        Toast.LENGTH_SHORT
                    ).show()
                    input.text.clear()
                }
            }
        }

        layout.addView(input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(enterBtn)

        setContentView(layout)
    }
}
