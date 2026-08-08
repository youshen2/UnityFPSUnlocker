package io.github.hexstr.UnityFPSUnlocker.ui

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.hexstr.UnityFPSUnlocker.ModulePreferences
import io.github.hexstr.UnityFPSUnlocker.apps.InstalledApp
import io.github.hexstr.UnityFPSUnlocker.apps.MIUI_INSTALLED_APPS_PERMISSION
import io.github.hexstr.UnityFPSUnlocker.apps.loadInstalledApps
import io.github.hexstr.UnityFPSUnlocker.apps.supportsMiuiInstalledAppsPermission
import io.github.hexstr.UnityFPSUnlocker.display.loadDisplayModes
import io.github.hexstr.UnityFPSUnlocker.ui.screens.AboutScreen
import io.github.hexstr.UnityFPSUnlocker.ui.screens.AppManagerScreen
import io.github.hexstr.UnityFPSUnlocker.ui.screens.AppSettingsScreen
import io.github.hexstr.UnityFPSUnlocker.ui.screens.GlobalSettingsScreen
import io.github.hexstr.UnityFPSUnlocker.ui.screens.LogsScreen

private sealed interface SettingsScreen {
    data object Global : SettingsScreen
    data object Apps : SettingsScreen
    data object Logs : SettingsScreen
    data object About : SettingsScreen
    data class AppDetails(val app: InstalledApp) : SettingsScreen
}

private val SettingsScreen.depth: Int
    get() = when (this) {
        SettingsScreen.Global -> 0
        SettingsScreen.Apps, SettingsScreen.Logs, SettingsScreen.About -> 1
        is SettingsScreen.AppDetails -> 2
    }

@Composable
fun SettingsApp() {
    val context = LocalContext.current
    val activity = context as Activity
    val repository = remember { ModulePreferences(context) }
    val displayModes = remember { loadDisplayModes(activity) }

    var globalConfig by remember { mutableStateOf(repository.readGlobal()) }
    var onlyManagedApps by remember { mutableStateOf(repository.onlyManagedApps()) }
    var managedPackages by remember { mutableStateOf(repository.managedPackages()) }
    var managedCount by remember { mutableIntStateOf(repository.enabledManagedCount()) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loadingApps by remember { mutableStateOf(false) }
    var appsLoadError by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf<SettingsScreen>(SettingsScreen.Global) }
    var appLoadRequest by remember { mutableIntStateOf(0) }
    var permissionRequestedThisSession by remember { mutableStateOf(false) }
    val supportsRuntimeAppListPermission = remember {
        supportsMiuiInstalledAppsPermission(context)
    }

    val appListPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { grantedByCallback ->
        val granted = grantedByCallback || ContextCompat.checkSelfPermission(
            context,
            MIUI_INSTALLED_APPS_PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED
        screen = SettingsScreen.Apps
        if (granted && installedApps.isEmpty()) {
            appLoadRequest++
        } else {
            loadingApps = false
            appsLoadError = "未授予“获取应用列表”权限，请在系统权限管理中允许后重试"
        }
    }

    LaunchedEffect(appLoadRequest) {
        if (appLoadRequest == 0) return@LaunchedEffect
        loadingApps = true
        appsLoadError = null
        runCatching { loadInstalledApps(context) }
            .onSuccess { installedApps = it }
            .onFailure {
                appsLoadError = it.localizedMessage ?: "系统拒绝读取应用列表"
            }
        loadingApps = false
    }

    fun openAppManager() {
        val permissionGranted = !supportsRuntimeAppListPermission ||
            ContextCompat.checkSelfPermission(
                context,
                MIUI_INSTALLED_APPS_PERMISSION,
            ) == PackageManager.PERMISSION_GRANTED

        when {
            permissionGranted -> {
                screen = SettingsScreen.Apps
                if (installedApps.isEmpty()) appLoadRequest++
            }

            !permissionRequestedThisSession -> {
                permissionRequestedThisSession = true
                appListPermissionLauncher.launch(MIUI_INSTALLED_APPS_PERMISSION)
            }

            else -> {
                screen = SettingsScreen.Apps
                loadingApps = false
                appsLoadError = "未授予“获取应用列表”权限，请在系统权限管理中允许后重试"
            }
        }
    }

    fun refreshManagedPackages() {
        managedPackages = repository.managedPackages()
        managedCount = repository.enabledManagedCount()
    }

    BackHandler(enabled = screen != SettingsScreen.Global) {
        screen = when (screen) {
            is SettingsScreen.AppDetails -> SettingsScreen.Apps
            SettingsScreen.Apps, SettingsScreen.Logs, SettingsScreen.About -> SettingsScreen.Global
            SettingsScreen.Global -> SettingsScreen.Global
        }
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (targetState.depth > initialState.depth) {
                (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> -width / 4 } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> width / 4 } + fadeOut())
            }
        },
        label = "settings-page-transition",
    ) { current ->
        when (current) {
            SettingsScreen.Global -> GlobalSettingsScreen(
                config = globalConfig,
                onlyManagedApps = onlyManagedApps,
                managedCount = managedCount,
                displayModes = displayModes,
                onConfigChange = { next ->
                    globalConfig = next
                    repository.writeGlobal(next)
                },
                onOnlyManagedAppsChange = { enabled ->
                    onlyManagedApps = enabled
                    repository.setOnlyManagedApps(enabled)
                },
                onOpenApps = ::openAppManager,
                onOpenLogs = { screen = SettingsScreen.Logs },
                onOpenAbout = { screen = SettingsScreen.About },
            )

            SettingsScreen.Apps -> AppManagerScreen(
                installedApps = installedApps,
                loading = loadingApps,
                loadError = appsLoadError,
                managedPackages = managedPackages,
                repository = repository,
                onBack = { screen = SettingsScreen.Global },
                onOpenApp = { app -> screen = SettingsScreen.AppDetails(app) },
            )

            SettingsScreen.About -> AboutScreen(
                onBack = { screen = SettingsScreen.Global },
            )

            SettingsScreen.Logs -> LogsScreen(
                onBack = { screen = SettingsScreen.Global },
            )

            is SettingsScreen.AppDetails -> AppSettingsScreen(
                app = current.app,
                globalConfig = globalConfig,
                displayModes = displayModes,
                repository = repository,
                onBack = { screen = SettingsScreen.Apps },
                onPolicyChanged = ::refreshManagedPackages,
            )
        }
    }
}
