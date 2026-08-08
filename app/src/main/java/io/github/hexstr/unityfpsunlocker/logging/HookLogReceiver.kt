package io.github.hexstr.UnityFPSUnlocker.logging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.hexstr.UnityFPSUnlocker.preferences.PreferenceContract

class HookLogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != HookLogContract.ACTION) return
        val expectedToken = context.getSharedPreferences(
            PreferenceContract.PREFS_NAME,
            Context.MODE_PRIVATE,
        ).getString(PreferenceContract.KEY_HOOK_LOG_TOKEN, null) ?: return
        if (intent.getStringExtra(HookLogContract.EXTRA_PROTOCOL_TOKEN) != expectedToken) return

        val packageName = intent.getStringExtra(HookLogContract.EXTRA_PACKAGE_NAME)
            ?.takeIf(PreferenceContract::isValidPackageName)
            ?: return
        if (Build.VERSION.SDK_INT >= 34 && sentFromPackage != packageName) return

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

    private companion object {
        const val MAX_MESSAGE_LENGTH = 500
    }
}
