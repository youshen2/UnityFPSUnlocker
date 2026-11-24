package io.github.hexstr.UnityFPSUnlocker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.hexstr.UnityFPSUnlocker.config.AppPolicy
import io.github.hexstr.UnityFPSUnlocker.config.ModuleConfig
import io.github.hexstr.UnityFPSUnlocker.apps.AppIconLoader
import io.github.hexstr.UnityFPSUnlocker.apps.InstalledApp
import io.github.hexstr.UnityFPSUnlocker.display.DisplayModeOption
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class ConfigField {
    Delay,
    Fps,
    Scale,
}

@Composable
internal fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        SmallTitle(text = title, modifier = Modifier.padding(top = 12.dp))
        Card(
            modifier = Modifier.padding(horizontal = 12.dp),
            insideMargin = PaddingValues(0.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun ToggleSetting(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    BasicComponent(
        title = title,
        summary = summary,
        endActions = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        onClick = { onCheckedChange(!checked) },
    )
}

@Composable
internal fun OptionalValueSetting(
    title: String,
    summary: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = summary,
        endActions = {
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        },
        onClick = onClick,
    )
}

@Composable
internal fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.padding(start = 18.dp)) {
        Icon(
            imageVector = MiuixIcons.Back,
            contentDescription = "返回",
        )
    }
}

@Composable
internal fun AppIcon(app: InstalledApp, size: Int = 44) {
    val context = LocalContext.current
    val icon by produceState(
        initialValue = AppIconLoader.cached(app.packageName),
        app.packageName,
    ) {
        value = AppIconLoader.load(context.applicationContext, app.packageName)
    }
    if (icon != null) {
        Image(
            bitmap = icon!!,
            contentDescription = null,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.firstOrNull()?.uppercase() ?: "?",
                color = MiuixTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

internal data class NumberFieldSpec(
    val title: String,
    val summary: String,
    val initialValue: String,
    val decimal: Boolean,
    val validator: (String) -> Boolean,
)

internal fun numberFieldSpec(
    field: ConfigField,
    config: ModuleConfig,
): NumberFieldSpec = when (field) {
    ConfigField.Delay -> NumberFieldSpec(
        title = "注入延迟",
        summary = "允许范围 1–120 秒；是否等待由设置项右侧开关控制",
        initialValue = config.delay.takeIf { it > 0 }
            ?.toString()
            ?: ModuleConfig.DEFAULT_DELAY.toString(),
        decimal = false,
        validator = { it.toIntOrNull() in 1..120 },
    )

    ConfigField.Fps -> NumberFieldSpec(
        title = "目标 FPS",
        summary = "允许范围 1–1000 FPS；是否修改由设置项右侧开关控制",
        initialValue = config.fps.takeIf { it > 0 }
            ?.toString()
            ?: ModuleConfig.DEFAULT_FPS.toString(),
        decimal = false,
        validator = { it.toIntOrNull() in 1..1000 },
    )

    ConfigField.Scale -> NumberFieldSpec(
        title = "渲染缩放",
        summary = "允许范围 0.1–4.0；是否修改由设置项右侧开关控制",
        initialValue = config.scale.takeIf { it > 0f }?.toString() ?: "1.0",
        decimal = true,
        validator = {
            val value = it.toFloatOrNull()
            value != null && value in 0.1f..4f
        },
    )
}

@Composable
internal fun NumberEditorDialog(
    spec: NumberFieldSpec,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(spec.title, spec.initialValue) {
        mutableStateOf(spec.initialValue)
    }
    val valid = spec.validator(value)

    OverlayDialog(
        show = true,
        title = spec.title,
        summary = spec.summary,
        onDismissRequest = onDismiss,
    ) {
        TextField(
            value = value,
            onValueChange = { candidate ->
                value = if (spec.decimal) {
                    candidate.filter { char ->
                        char.isDigit() || char == '.'
                    }
                } else {
                    candidate.filter(Char::isDigit)
                }
            },
            label = spec.title,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (spec.decimal) KeyboardType.Decimal else KeyboardType.Number,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(20.dp))
            TextButton(
                text = "确定",
                onClick = { onConfirm(value) },
                enabled = valid,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun DisplayModeDialog(
    show: Boolean,
    selectedId: Int,
    displayModes: List<DisplayModeOption>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    OverlayDialog(
        show = show,
        title = "显示模式",
        summary = "是否修改显示模式由设置项右侧开关控制",
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            displayModes.filter { it.id > 0 }.forEach { mode ->
                BasicComponent(
                    title = mode.label,
                    summary = if (mode.id == selectedId) "当前选择" else null,
                    titleColor = if (mode.id == selectedId) {
                        BasicComponentDefaults.titleColor(
                            color = MiuixTheme.colorScheme.primary,
                        )
                    } else {
                        BasicComponentDefaults.titleColor()
                    },
                    onClick = { onSelect(mode.id) },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            text = "取消",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun fpsSummary(fps: Int, enabled: Boolean = fps > 0): String =
    if (enabled) "$fps FPS" else "不修改"

internal fun delaySummary(delay: Int, enabled: Boolean = delay > 0): String =
    if (enabled) "$delay 秒" else "不等待"

internal fun scaleSummary(scale: Float, enabled: Boolean = scale > 0f): String =
    if (enabled) "${scale}×" else "不修改"

internal fun displayModeLabel(
    displayModeId: Int,
    displayModes: List<DisplayModeOption>,
    enabled: Boolean = displayModeId > 0,
): String = when {
    !enabled -> "不修改"
    else -> displayModes.firstOrNull { it.id == displayModeId }?.label
        ?: "模式 ID $displayModeId（当前设备不可用）"
}

internal fun policySummary(policy: AppPolicy): String = when {
    !policy.managed -> policy.packageName
    !policy.enabled -> "${policy.packageName} · 已禁用"
    policy.useGlobal -> "${policy.packageName} · 使用全局默认值"
    else -> "${policy.packageName} · ${fpsSummary(policy.config.fps, policy.config.fpsEnabled)} · 独立配置"
}
