package io.github.hexstr.UnityFPSUnlocker.logging

enum class HookLogStatus(val wireName: String) {
    Success(HookLogContract.STATUS_SUCCESS),
    Failed(HookLogContract.STATUS_FAILED),
    Skipped(HookLogContract.STATUS_SKIPPED);

    companion object {
        fun fromWireName(value: String?): HookLogStatus? =
            entries.firstOrNull { it.wireName == value }
    }
}

data class HookLogEntry(
    val sequence: Long,
    val packageName: String,
    val status: HookLogStatus,
    val timestamp: Long,
    val message: String,
)

data class HookLogSnapshot(
    val entries: List<HookLogEntry>,
    val capturedAt: Long = System.currentTimeMillis(),
)
