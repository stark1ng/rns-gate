package com.rnsgate.app

import android.app.Application
import com.rnsgate.app.data.LxmfMessenger
import com.rnsgate.app.data.RnsNode
import com.rnsgate.app.data.SettingsStore
import com.rnsgate.app.data.demo.DemoLxmfMessenger
import com.rnsgate.app.data.demo.DemoRnsNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RnsGateApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settingsStore: SettingsStore
        private set
    lateinit var rnsNode: RnsNode
        private set
    lateinit var messenger: LxmfMessenger
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        rnsNode = DemoRnsNode(appScope, settingsStore)
        messenger = DemoLxmfMessenger(appScope)
    }
}

fun Application.asRnsGate(): RnsGateApp = this as RnsGateApp
