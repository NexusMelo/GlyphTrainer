package pt.vicktor.glyphon

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

class MainActivity : Activity() {
    private val OVERLAY_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        if (!hasOverlayPermission) {
            setTheme(R.style.Theme_GlyphTrainer_Permission)
        }

        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_MAIN) {
            AppMode.currentMode = AppMode.Mode.PLAY
        }

        if (hasOverlayPermission) {
            startOverlay()
        } else {
            showOverlayPermissionExplanation()
        }
    }

    private fun showOverlayPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.overlay_permission_title)
            .setMessage(R.string.overlay_permission_message)
            .setPositiveButton(R.string.action_continue) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivityForResult(intent, OVERLAY_REQ_CODE)
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OVERLAY_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) startOverlay()
            else finish()
        }
    }

    private fun startOverlay() {
        startService(Intent(this, OverlayService::class.java))
        finish()
    }
}
