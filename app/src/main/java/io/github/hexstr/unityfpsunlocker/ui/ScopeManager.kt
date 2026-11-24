package io.github.hexstr.UnityFPSUnlocker.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.hexstr.UnityFPSUnlocker.App
import io.github.libxposed.service.XposedService

/** 通过 XposedService 动态增删模块作用域。 */
object ScopeManager {

    fun request(context: Context, packageName: String) {
        val service = App.service
        if (service == null) {
            toast(context, "框架未激活：已保存配置，请在框架中手动勾选 $packageName")
            return
        }

        runCatching {
            service.requestScope(
                listOf(packageName),
                object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(packages: MutableList<String>) {
                        toast(context, "已添加作用域：$packageName，重启目标应用后生效")
                    }

                    override fun onScopeRequestFailed(message: String) {
                        toast(context, "申请作用域失败：$message")
                    }
                },
            )
        }.onFailure {
            toast(context, "申请作用域失败：${it.message ?: it.javaClass.simpleName}")
        }
    }

    /**
     * 移除作用域，并在框架更新后回查结果。
     *
     * callback 参数：true = 仍在作用域，false = 已移出，null = 无法查询。
     */
    fun remove(
        context: Context,
        packageName: String,
        onChecked: ((Boolean?) -> Unit)? = null,
    ) {
        val service = App.service
        if (service == null) {
            toast(context, "框架未激活，无法检查作用域")
            onChecked?.invoke(null)
            return
        }

        runCatching {
            service.removeScope(listOf(packageName))
        }.onFailure {
            toast(context, "移除作用域失败：${it.message ?: it.javaClass.simpleName}")
            onChecked?.invoke(null)
            return
        }

        awaitScopeRemoval(context, packageName, attempt = 0, onChecked = onChecked)
    }

    fun isInScope(packageName: String): Boolean? {
        val service = App.service ?: return null
        return runCatching { service.scope.contains(packageName) }.getOrNull()
    }

    private fun awaitScopeRemoval(
        context: Context,
        packageName: String,
        attempt: Int,
        onChecked: ((Boolean?) -> Unit)?,
    ) {
        Handler(Looper.getMainLooper()).postDelayed({
            val inScope = isInScope(packageName)
            when {
                inScope == false -> {
                    toast(context, "已移除作用域：$packageName")
                    onChecked?.invoke(false)
                }

                inScope == null || attempt >= MAX_ATTEMPTS -> {
                    if (inScope == true) {
                        toast(context, "作用域未移除，已保留禁用状态：$packageName")
                    }
                    onChecked?.invoke(inScope)
                }

                else -> awaitScopeRemoval(context, packageName, attempt + 1, onChecked)
            }
        }, CHECK_INTERVAL_MS)
    }

    private fun toast(context: Context, message: String) {
        val applicationContext = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private const val CHECK_INTERVAL_MS = 300L
    private const val MAX_ATTEMPTS = 10
}
