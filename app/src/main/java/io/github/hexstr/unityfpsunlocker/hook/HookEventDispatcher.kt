package io.github.hexstr.UnityFPSUnlocker.hook

import android.app.BroadcastOptions
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.hexstr.UnityFPSUnlocker.BuildConfig
import io.github.hexstr.UnityFPSUnlocker.config.KEY_HOOK_LOG_TOKEN
import io.github.hexstr.UnityFPSUnlocker.logging.HookLogContract
import java.lang.ref.WeakReference
import java.util.ArrayDeque

/**
 * 在被 Hook 进程中收集事件，并通过显式广播回传给模块设置界面。
 *
 * 使用 Application.attach 获取目标进程 Context；Context 就绪前的事件先排队。
 */
object HookEventDispatcher {

    private const val TAG = "UnityFPSUnlocker"
    private const val MAX_PENDING = 64

    private val lock = Any()
    private val pending = ArrayDeque<PendingEvent>()

    private var contextRef = WeakReference<Context>(null)
    private var token: String? = null
    private var installed = false

    fun initialize(prefs: SharedPreferences) {
        synchronized(lock) {
            token = prefs.getString(KEY_HOOK_LOG_TOKEN, null)
        }
    }

    fun install(module: XposedModule, classLoader: ClassLoader) {
        synchronized(lock) {
            if (installed) return
            installed = true
        }
        runCatching {
            val application = Class.forName("android.app.Application", false, classLoader)
            val attach = application.getDeclaredMethod("attach", Context::class.java)
            module.hook(attach).intercept { chain ->
                val result = chain.proceed()
                (chain.args.firstOrNull() as? Context)?.let(::setContext)
                result
            }
        }.onFailure {
            module.log(Log.ERROR, TAG, "Failed to install context hook", it)
        }
    }

    fun setContext(context: Context) {
        val applicationContext = context.applicationContext ?: context
        val queued: List<PendingEvent>
        synchronized(lock) {
            contextRef = WeakReference(applicationContext)
            queued = pending.toList()
            pending.clear()
        }
        queued.forEach { send(applicationContext, it) }
    }

    @JvmStatic
    fun report(packageName: String, status: String, message: String) {
        val event = PendingEvent(packageName, status, message)
        val context = synchronized(lock) {
            val current = contextRef.get()
            if (current == null) {
                if (pending.size >= MAX_PENDING) pending.removeFirst()
                pending.addLast(event)
                null
            } else {
                current
            }
        } ?: return
        send(context, event)
    }

    private fun send(context: Context, event: PendingEvent) {
        try {
            val currentToken = synchronized(lock) { token }
            if (currentToken.isNullOrEmpty()) return

            val intent = Intent(HookLogContract.ACTION)
                .setComponent(
                    ComponentName(
                        BuildConfig.APPLICATION_ID,
                        HookLogContract.RECEIVER_CLASS,
                    ),
                )
                .putExtra(HookLogContract.EXTRA_TOKEN, currentToken)
                .putExtra(HookLogContract.EXTRA_PACKAGE_NAME, event.packageName)
                .putExtra(HookLogContract.EXTRA_STATUS, event.status)
                .putExtra(HookLogContract.EXTRA_MESSAGE, event.message)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.sendBroadcast(
                    intent,
                    null,
                    BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle(),
                )
            } else {
                context.sendBroadcast(intent)
            }
        } catch (throwable: Throwable) {
            Log.w(TAG, "Failed to deliver hook event", throwable)
        }
    }

    private data class PendingEvent(
        val packageName: String,
        val status: String,
        val message: String,
    )
}
