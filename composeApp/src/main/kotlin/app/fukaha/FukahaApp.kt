package app.fukaha

import android.app.Application
import app.fukaha.android.LocaleHelper
import kotlinx.coroutines.runBlocking

class FukahaApp : Application() {
    lateinit var settingsStore: AndroidSettingsStore
        private set

    lateinit var bridge: FukahaBridge
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = AndroidSettingsStore(this)
        bridge = FukahaBridge()
        runBlocking {
            LocaleHelper.apply(settingsStore.get().language)
        }
    }
}

fun Application.fukaha(): FukahaApp = this as FukahaApp
