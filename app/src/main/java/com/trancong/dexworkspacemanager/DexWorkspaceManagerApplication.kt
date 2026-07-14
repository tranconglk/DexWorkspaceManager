package com.trancong.dexworkspacemanager

import android.app.Application

class DexWorkspaceManagerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
