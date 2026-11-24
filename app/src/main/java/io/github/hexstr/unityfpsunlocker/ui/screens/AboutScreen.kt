package io.github.hexstr.UnityFPSUnlocker.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.hexstr.UnityFPSUnlocker.BuildConfig
import io.github.hexstr.UnityFPSUnlocker.R
import io.github.hexstr.UnityFPSUnlocker.ui.components.BackButton
import io.github.hexstr.UnityFPSUnlocker.ui.components.SettingsSection
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val upstreamUrl = "https://github.com/hexstr/UnityFPSUnlocker"
    val sourceUrl = "https://github.com/youshen2/UnityFPSUnlocker"

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "关于",
                largeTitle = "关于",
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(84.dp),
                    )
                    Text(
                        text = "Unity FPS Unlocker",
                        style = MiuixTheme.textStyles.title2,
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            item {
                SettingsSection(title = "作者") {
                    BasicComponent(
                        title = "原作者",
                        summary = "hexstr",
                    )
                    BasicComponent(
                        title = "二改作者",
                        summary = "爅峫",
                    )
                }
            }

            item {
                SettingsSection(title = "项目") {
                    ArrowPreference(
                        title = "上游项目",
                        summary = upstreamUrl,
                        onClick = { openUrl(upstreamUrl) },
                    )
                    ArrowPreference(
                        title = "源码仓库",
                        summary = sourceUrl,
                        onClick = { openUrl(sourceUrl) },
                    )
                    BasicComponent(
                        title = "开源许可",
                        summary = "MIT License",
                    )
                }
            }
        }
    }
}
