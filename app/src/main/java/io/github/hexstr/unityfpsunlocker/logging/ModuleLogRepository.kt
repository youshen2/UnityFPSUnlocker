package io.github.hexstr.UnityFPSUnlocker.logging

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object ModuleLogRepository {
    suspend fun readLogs(context: Context): ModuleLogSnapshot = withContext(Dispatchers.IO) {
        HookLogStore.read(context.applicationContext)
    }
}
