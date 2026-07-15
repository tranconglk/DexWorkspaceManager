package com.trancong.dexworkspacemanager.platform.installedapps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AndroidPackageChangeMonitor(context: Context) : PackageChangeMonitor {
    private val applicationContext = context.applicationContext
    private val mutableEvents = MutableSharedFlow<PackageChangeEvent>(
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events: Flow<PackageChangeEvent> = mutableEvents.asSharedFlow()

    private var activeClients = 0
    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val packageName = intent.data?.schemeSpecificPart?.takeIf(String::isNotBlank) ?: return
            val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            val changeType = when (action) {
                Intent.ACTION_PACKAGE_REMOVED -> {
                    if (replacing) return
                    PackageChangeType.REMOVED
                }
                Intent.ACTION_PACKAGE_ADDED -> {
                    if (replacing) return
                    PackageChangeType.ADDED
                }
                Intent.ACTION_PACKAGE_REPLACED -> PackageChangeType.REPLACED
                Intent.ACTION_PACKAGE_CHANGED -> PackageChangeType.CHANGED
                else -> return
            }
            mutableEvents.tryEmit(PackageChangeEvent(packageName, changeType))
        }
    }

    @Synchronized
    override fun start() {
        activeClients += 1
        if (isRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme(PACKAGE_SCHEME)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            applicationContext.registerReceiver(receiver, filter)
        }
        isRegistered = true
    }

    @Synchronized
    override fun stop() {
        if (activeClients == 0) return
        activeClients -= 1
        if (activeClients > 0 || !isRegistered) return

        applicationContext.unregisterReceiver(receiver)
        isRegistered = false
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 16
        const val PACKAGE_SCHEME = "package"
    }
}
