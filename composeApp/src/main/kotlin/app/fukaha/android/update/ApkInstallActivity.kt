package app.fukaha.android.update

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import app.fukaha.R

/**
 * Receives [PackageInstaller.Session.commit] status. The confirm sheet must be
 * started from an Activity (see STATUS_PENDING_USER_ACTION / EXTRA_INTENT).
 */
class ApkInstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        finish()
    }

    private fun handle(intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = confirmIntent(intent) ?: return
                startActivity(confirm)
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.update_failed)
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val ACTION_STATUS = "app.fukaha.android.update.INSTALL_STATUS"

        fun confirmIntent(intent: Intent): Intent? {
            return parcelableIntent(intent, Intent.EXTRA_INTENT)
                ?: parcelableIntent(intent, "android.content.pm.extra.INTENT")
        }

        private fun parcelableIntent(intent: Intent, key: String): Intent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(key, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(key)
            }
    }
}
