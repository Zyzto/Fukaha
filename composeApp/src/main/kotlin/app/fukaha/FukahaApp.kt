package app.fukaha

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.fukaha.android.EmbedHealthController
import app.fukaha.android.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

class FukahaApp : Application() {
    @Volatile
    var lastKnownSettings: FukahaSettings = FukahaSettings()
        private set

    lateinit var settingsStore: AndroidSettingsStore
        private set

    lateinit var healthStore: AndroidEmbedHealthStore
        private set

    lateinit var healthController: EmbedHealthController
        private set

    lateinit var bridge: FukahaBridge
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = AndroidSettingsStore(this)
        healthStore = AndroidEmbedHealthStore(this)
        healthController = EmbedHealthController(
            store = healthStore,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
        bridge = FukahaBridge()
        runBlocking {
            lastKnownSettings = settingsStore.get()
            LocaleHelper.apply(lastKnownSettings.language)
        }

        // Probes are started by MainActivity. Cancel only after a real background
        // stay — a same-process jump to ShareActivity is not leaving the app.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    healthController.stayForegrounded()
                }

                override fun onStop(owner: LifecycleOwner) {
                    healthController.scheduleCancelIfBackgrounded()
                }
            },
        )
    }

    fun noteSettings(settings: FukahaSettings) {
        lastKnownSettings = settings
    }
}

fun Application.fukaha(): FukahaApp = this as FukahaApp
