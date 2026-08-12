package app.fukaha.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import app.fukaha.FukahaSettings
import app.fukaha.MediaDownloadResult
import app.fukaha.PreparedLink
import app.fukaha.R
import app.fukaha.ShareAction
import app.fukaha.android.share.AutoActionProgress
import app.fukaha.android.share.ShareSheet
import app.fukaha.android.theme.FukahaTheme
import app.fukaha.fukaha
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = intent.extractSharedText()
        val app = application.fukaha()

        setContent {
            var settings by remember { mutableStateOf(FukahaSettings()) }
            var prepared by remember { mutableStateOf<PreparedLink?>(null) }
            var loading by remember { mutableStateOf(true) }
            var downloading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(sharedText) {
                settings = app.settingsStore.get()
                if (sharedText.isNullOrBlank()) {
                    error = getString(R.string.no_link)
                    loading = false
                    return@LaunchedEffect
                }
                prepared = withContext(Dispatchers.IO) {
                    app.bridge.prepare(sharedText, settings)
                }
                if (prepared == null) {
                    error = getString(R.string.no_link)
                    loading = false
                    return@LaunchedEffect
                }
                loading = false

                when (settings.defaultAction) {
                    ShareAction.Ask -> Unit
                    ShareAction.Clean -> {
                        shareText(prepared!!.detected.cleanedUrl)
                        finish()
                    }
                    ShareAction.Embed -> {
                        shareText(prepared!!.embedUrl ?: prepared!!.detected.cleanedUrl)
                        finish()
                    }
                    ShareAction.Download -> {
                        downloading = true
                        val result = withContext(Dispatchers.IO) {
                            app.bridge.download(
                                prepared!!.detected.cleanedUrl,
                                settings,
                                mediaCacheDir().absolutePath,
                            )
                        }
                        downloading = false
                        when (result) {
                            is MediaDownloadResult.Success -> {
                                shareFile(result.filePath, result.mimeType)
                                if (settings.deleteCacheAfterShare) File(result.filePath).delete()
                                finish()
                            }
                            is MediaDownloadResult.Failure -> {
                                Toast.makeText(
                                    this@ShareActivity,
                                    "${getString(R.string.download_failed)}: ${result.message}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                shareText(prepared!!.embedUrl ?: prepared!!.detected.cleanedUrl)
                                finish()
                            }
                        }
                    }
                }
            }

            FukahaTheme(theme = settings.theme) {
                if (settings.defaultAction != ShareAction.Ask && error == null) {
                    AutoActionProgress()
                } else {
                    ShareSheet(
                        loading = loading,
                        downloading = downloading,
                        error = error,
                        prepared = prepared,
                        onDismiss = { finish() },
                        onShareCleaned = {
                            prepared?.let {
                                shareText(it.detected.cleanedUrl)
                                finish()
                            }
                        },
                        onShareEmbed = {
                            prepared?.let {
                                shareText(it.embedUrl ?: it.detected.cleanedUrl)
                                finish()
                            }
                        },
                        onShareMedia = {
                            val link = prepared ?: return@ShareSheet
                            scope.launch {
                                downloading = true
                                val result = withContext(Dispatchers.IO) {
                                    app.bridge.download(
                                        link.detected.cleanedUrl,
                                        settings,
                                        mediaCacheDir().absolutePath,
                                    )
                                }
                                downloading = false
                                when (result) {
                                    is MediaDownloadResult.Success -> {
                                        shareFile(result.filePath, result.mimeType)
                                        if (settings.deleteCacheAfterShare) {
                                            File(result.filePath).delete()
                                        }
                                        finish()
                                    }
                                    is MediaDownloadResult.Failure -> {
                                        Toast.makeText(
                                            this@ShareActivity,
                                            "${getString(R.string.download_failed)}: ${result.message}",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        if (link.embedUrl != null) {
                                            Toast.makeText(
                                                this@ShareActivity,
                                                getString(R.string.fallback_embed),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            shareText(link.embedUrl!!)
                                            finish()
                                        }
                                    }
                                }
                            }
                        },
                        onCopyCleaned = {
                            prepared?.detected?.cleanedUrl?.let { copyText(it) }
                        },
                        onCopyEmbed = {
                            prepared?.embedUrl?.let { copyText(it) }
                        },
                    )
                }
            }
        }
    }

    private fun mediaCacheDir(): File {
        val dir = File(cacheDir, "fukaha")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, getString(R.string.app_name)))
    }

    private fun shareFile(path: String, mimeType: String?) {
        val file = File(path)
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType ?: "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("", uri)
        }
        startActivity(Intent.createChooser(send, getString(R.string.app_name)))
    }

    private fun copyText(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("fukaha", text))
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }
}

private fun Intent.extractSharedText(): String? {
    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
    return getStringExtra(Intent.EXTRA_TEXT)
        ?: getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        ?: getStringExtra(Intent.EXTRA_SUBJECT)
}
