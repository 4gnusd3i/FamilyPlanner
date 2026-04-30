package io.github.by4gnusd3i.familyplanner.data.avatar

import java.util.Locale

object AvatarStoragePolicy {
    const val MaxAvatarBytes: Long = 5L * 1024L * 1024L

    fun extensionForMimeType(mimeType: String?): String {
        val normalized = mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?: throw IllegalArgumentException("avatar must be an image")

        require(normalized.startsWith("image/")) {
            "avatar must be an image"
        }

        return when (val subtype = normalized.removePrefix("image/")) {
            "jpeg", "pjpeg" -> "jpg"
            "png", "webp", "gif", "heic", "heif" -> subtype
            else -> subtype
                .filter { it.isLetterOrDigit() }
                .takeIf { it.isNotBlank() && it.length <= 8 }
                ?: "img"
        }
    }
}
