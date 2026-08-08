package io.github.hexstr.UnityFPSUnlocker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.hexstr.UnityFPSUnlocker.AppPolicy
import io.github.hexstr.UnityFPSUnlocker.ModulePreferences
import io.github.hexstr.UnityFPSUnlocker.apps.InstalledApp
import io.github.hexstr.UnityFPSUnlocker.ui.components.AppIcon
import io.github.hexstr.UnityFPSUnlocker.ui.components.BackButton
import io.github.hexstr.UnityFPSUnlocker.ui.components.ToggleSetting
import io.github.hexstr.UnityFPSUnlocker.ui.components.policySummary
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun AppManagerScreen(
    installedApps: List<InstalledApp>,
    loading: Boolean,
    loadError: String?,
    managedPackages: Set<String>,
    repository: ModulePreferences,
    onBack: () -> Unit,
    onOpenApp: (InstalledApp) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    var query by remember { mutableStateOf("") }
    var includeSystemApps by remember { mutableStateOf(false) }

    val missingManagedApps = remember(installedApps, managedPackages) {
        managedPackages
            .minus(installedApps.mapTo(mutableSetOf()) { it.packageName })
            .map { packageName ->
                InstalledApp(
                    label = packageName,
                    packageName = packageName,
                    systemApp = false,
                )
            }
    }
    val normalizedQuery = remember(query) { query.trim() }
    val visibleApps = remember(
        installedApps,
        missingManagedApps,
        includeSystemApps,
        normalizedQuery,
        managedPackages,
    ) {
        (installedApps + missingManagedApps)
            .asSequence()
            .filter { app ->
                includeSystemApps || !app.systemApp || app.packageName in managedPackages
            }
            .filter { app ->
                normalizedQuery.isEmpty() ||
                    app.label.contains(normalizedQuery, ignoreCase = true) ||
                    app.packageName.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedWith(compareBy<InstalledApp> { it.label.lowercase() }.thenBy { it.packageName })
            .toList()
    }
    val policies = remember(visibleApps, managedPackages, repository) {
        repository.readAppPolicies(visibleApps.map(InstalledApp::packageName))
    }
    val configuredPackages = remember(visibleApps, policies) {
        visibleApps.asSequence()
            .filter { app ->
                policies[app.packageName]?.let { it.managed && it.enabled } == true
            }
            .mapTo(mutableSetOf(), InstalledApp::packageName)
    }
    val configuredApps = remember(visibleApps, configuredPackages) {
        visibleApps.filter { it.packageName in configuredPackages }
    }
    val otherApps = remember(visibleApps, configuredPackages) {
        visibleApps.filterNot { it.packageName in configuredPackages }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "应用管理器",
                largeTitle = "应用管理器",
                scrollBehavior = scrollBehavior,
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 40.dp,
            ),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "搜索应用或包名",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(insideMargin = PaddingValues(0.dp)) {
                        ToggleSetting(
                            title = "显示系统应用",
                            summary = "默认只显示用户安装的应用",
                            checked = includeSystemApps,
                            onCheckedChange = { includeSystemApps = it },
                        )
                    }
                }
            }

            if (loadError != null) {
                item {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.defaultColors(
                            color = MiuixTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        BasicComponent(
                            title = "无法读取完整应用列表",
                            summary = "$loadError\n已声明应用列表权限，请确认安装的是最新构建。",
                        )
                    }
                }
            }

            when {
                loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                visibleApps.isEmpty() -> item {
                    Text(
                        text = "没有找到匹配的应用",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                    )
                }

                else -> {
                    if (configuredApps.isNotEmpty()) {
                        item {
                            SmallTitle(
                                text = "已配置 · ${configuredApps.size}",
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(configuredApps, key = { "configured:${it.packageName}" }) { app ->
                            val policy = policies.getValue(app.packageName)
                            AppManagerItem(
                                app = app,
                                policy = policy,
                                onOpen = { onOpenApp(app) },
                            )
                        }
                    }

                    if (otherApps.isNotEmpty()) {
                        item {
                            SmallTitle(
                                text = "其他应用 · ${otherApps.size}",
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(otherApps, key = { "available:${it.packageName}" }) { app ->
                            val policy = policies.getValue(app.packageName)
                            AppManagerItem(
                                app = app,
                                policy = policy,
                                onOpen = { onOpenApp(app) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppManagerItem(
    app: InstalledApp,
    policy: AppPolicy,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        BasicComponent(
            title = app.label,
            summary = policySummary(policy),
            startAction = { AppIcon(app = app) },
            onClick = onOpen,
        )
    }
}
