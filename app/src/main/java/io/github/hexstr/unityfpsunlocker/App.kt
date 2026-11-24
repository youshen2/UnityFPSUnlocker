package io.github.hexstr.UnityFPSUnlocker

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import io.github.hexstr.UnityFPSUnlocker.config.ConfigStore
import io.github.hexstr.UnityFPSUnlocker.config.HookLogToken
import io.github.hexstr.UnityFPSUnlocker.config.PREFS_GROUP
import kotlin.concurrent.Volatile

class App : Application(), XposedServiceHelper.OnServiceListener {

    companion object {
        /** 供 Compose 观察框架绑定状态 */
        val serviceState = mutableStateOf<XposedService?>(null)

        @Volatile
        var service: XposedService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        HookLogToken.ensure(this)
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(boundService: XposedService) {
        service = boundService
        Handler(Looper.getMainLooper()).post { serviceState.value = boundService }

        runCatching {
            val remote = boundService.getRemotePreferences(PREFS_GROUP)
            ConfigStore.migrateLocalToRemote(ConfigStore.localPreferences(this), remote)
            HookLogToken.ensure(this, remote)
        }
    }

    override fun onServiceDied(boundService: XposedService) {
        service = null
        Handler(Looper.getMainLooper()).post { serviceState.value = null }
    }
}
