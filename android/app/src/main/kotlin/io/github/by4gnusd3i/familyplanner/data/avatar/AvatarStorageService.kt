package io.github.by4gnusd3i.familyplanner.data.avatar

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class AvatarStorageService @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun storeAvatar(sourceUriValue: String?): String? {
        val sourceUri = sourceUriValue
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?: return null

        if (sourceUri.scheme == ContentResolver.SCHEME_FILE && isStoredAvatar(sourceUri)) {
            return sourceUriValue
        }

        require(sourceUri.scheme == ContentResolver.SCHEME_CONTENT) {
            "avatar must be selected from the Android photo picker"
        }

        val extension = AvatarStoragePolicy.extensionForMimeType(context.contentResolver.getType(sourceUri))
        val target = File(avatarDirectory(), "avatar-${UUID.randomUUID()}.$extension")

        context.contentResolver.openInputStream(sourceUri).use { input ->
            require(input != null) { "avatar could not be opened" }
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copied += read
                    if (copied > AvatarStoragePolicy.MaxAvatarBytes) {
                        target.delete()
                        throw IllegalArgumentException("avatar is too large")
                    }
                    output.write(buffer, 0, read)
                }
            }
        }

        return Uri.fromFile(target).toString()
    }

    fun deleteStoredAvatar(uriValue: String?) {
        val uri = uriValue
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?: return

        if (uri.scheme == ContentResolver.SCHEME_FILE && isStoredAvatar(uri)) {
            File(requireNotNull(uri.path)).delete()
        }
    }

    fun deleteAllStoredAvatars() {
        avatarDirectory().listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
    }

    private fun isStoredAvatar(uri: Uri): Boolean {
        val avatarDirectory = avatarDirectory().canonicalFile
        val avatarFile = uri.path?.let(::File)?.canonicalFile ?: return false
        return avatarFile.parentFile == avatarDirectory
    }

    private fun avatarDirectory(): File =
        File(context.filesDir, "avatars").apply { mkdirs() }
}
