package io.github.hexstr.UnityFPSUnlocker.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

data class InstalledApp(
    val label: String,
    val packageName: String,
    val systemApp: Boolean,
)

const val MIUI_INSTALLED_APPS_PERMISSION = "com.android.permission.GET_INSTALLED_APPS"

fun supportsMiuiInstalledAppsPermission(context: Context): Boolean = runCatching {
    @Suppress("DEPRECATION")
    context.packageManager.getPermissionInfo(MIUI_INSTALLED_APPS_PERMISSION, 0)
        .packageName == "com.lbe.security.miui"
}.getOrDefault(false)

suspend fun loadInstalledApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
    val packageManager = context.packageManager
    val applications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstalledApplications(0)
    }
    val collator = Collator.getInstance(Locale.getDefault())

    applications.asSequence()
        .filter { it.packageName != context.packageName && it.enabled }
        .map { info ->
            InstalledApp(
                label = runCatching { packageManager.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName),
                packageName = info.packageName,
                systemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            )
        }
        .sortedWith { left, right ->
            collator.compare(left.label, right.label).takeIf { it != 0 }
                ?: left.packageName.compareTo(right.packageName)
        }
        .toList()
}
