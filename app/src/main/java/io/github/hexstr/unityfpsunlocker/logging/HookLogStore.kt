package io.github.hexstr.UnityFPSUnlocker.logging

import android.content.Context
import org.json.JSONObject

/** 模块进程本地的环形日志存储，最多保留 [MAX_ENTRIES] 条记录。 */
object HookLogStore {
    const val MAX_ENTRIES = 200

    private const val PREFS_NAME = "hook_event_logs"
    private const val KEY_NEXT_SEQUENCE = "next_sequence"
    private const val ENTRY_PREFIX = "entry_"

    fun append(
        context: Context,
        packageName: String,
        status: HookLogStatus,
        message: String,
    ) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sequence = preferences.getLong(KEY_NEXT_SEQUENCE, 0L)
        val entry = JSONObject()
            .put("sequence", sequence)
            .put("packageName", packageName)
            .put("status", status.wireName)
            .put("timestamp", System.currentTimeMillis())
            .put("message", message)

        preferences.edit()
            .putString(entryKey(sequence), entry.toString())
            .putLong(KEY_NEXT_SEQUENCE, sequence + 1L)
            .apply()
    }

    fun read(context: Context): HookLogSnapshot {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextSequence = preferences.getLong(KEY_NEXT_SEQUENCE, 0L)
        val firstSequence = (nextSequence - MAX_ENTRIES).coerceAtLeast(0L)
        val entries = buildList {
            for (sequence in nextSequence - 1 downTo firstSequence) {
                val rawEntry = preferences.getString(entryKey(sequence), null) ?: continue
                parseEntry(rawEntry, sequence)?.let(::add)
            }
        }
        return HookLogSnapshot(entries = entries)
    }

    private fun parseEntry(rawEntry: String, expectedSequence: Long): HookLogEntry? =
        runCatching {
            val json = JSONObject(rawEntry)
            val sequence = json.getLong("sequence")
            if (sequence != expectedSequence) return null
            HookLogEntry(
                sequence = sequence,
                packageName = json.getString("packageName"),
                status = HookLogStatus.fromWireName(json.getString("status")) ?: return null,
                timestamp = json.getLong("timestamp"),
                message = json.getString("message"),
            )
        }.getOrNull()

    private fun entryKey(sequence: Long): String =
        "$ENTRY_PREFIX${sequence % MAX_ENTRIES}"
}
