package io.github.hexstr.UnityFPSUnlocker.logging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.hexstr.UnityFPSUnlocker.config.HookLogToken

class HookLogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != HookLogContract.ACTION) return

        val expectedToken = HookLogToken.local(context) ?: return
        if (intent.getStringExtra(HookLogContract.EXTRA_TOKEN) != expectedToken) return

        val packageName = intent.getStringExtra(HookLogContract.EXTRA_PACKAGE_NAME)
            ?.takeIf(::isValidPackageName)
            ?: return
        if (Build.VERSION.SDK_INT >= 34) {
            val sender = sentFromPackage
            if (sender != null && sender != packageName) return
        }

        val status = HookLogStatus.fromWireName(
            intent.getStringExtra(HookLogContract.EXTRA_STATUS),
        ) ?: return
        val message = intent.getStringExtra(HookLogContract.EXTRA_MESSAGE)
            ?.take(MAX_MESSAGE_LENGTH)
            ?: return

        HookLogStore.append(
            context = context.applicationContext,
            packageName = packageName,
            status = status,
            message = message,
        )
    }

    private fun isValidPackageName(value: String): Boolean =
        value.isNotEmpty() && value.all { it.isLetterOrDigit() || it == '.' || it == '_' }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 500
    }
}
