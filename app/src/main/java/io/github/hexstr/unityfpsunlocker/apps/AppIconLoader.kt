package io.github.hexstr.UnityFPSUnlocker.apps

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object AppIconLoader {
    private const val CACHE_SIZE_KIB = 8 * 1024
    private const val ICON_SIZE_DP = 48

    private val cache = object : LruCache<String, ImageBitmap>(CACHE_SIZE_KIB) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4 / 1024).coerceAtLeast(1)
    }

    fun cached(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun load(context: Context, packageName: String): ImageBitmap? =
        cached(packageName) ?: withContext(Dispatchers.IO) {
            runCatching {
                val iconSize = (ICON_SIZE_DP * context.resources.displayMetrics.density)
                    .toInt()
                    .coerceAtLeast(ICON_SIZE_DP)
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(iconSize, iconSize)
                    .asImageBitmap()
                    .also { cache.put(packageName, it) }
            }.getOrNull()
        }
}
