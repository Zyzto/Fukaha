package app.fukaha

import android.app.Application

class FukahaApp : Application() {
    lateinit var settingsStore: AndroidSettingsStore
        private set

    lateinit var bridge: FukahaBridge
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = AndroidSettingsStore(this)
        bridge = FukahaBridge()
    }
}

fun Application.fukaha(): FukahaApp = this as FukahaApp
