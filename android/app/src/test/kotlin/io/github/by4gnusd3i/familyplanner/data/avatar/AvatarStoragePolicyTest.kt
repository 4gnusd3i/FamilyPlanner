package io.github.by4gnusd3i.familyplanner.data.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AvatarStoragePolicyTest {
    @Test
    fun imageMimeTypesMapToStableExtensions() {
        assertEquals("jpg", AvatarStoragePolicy.extensionForMimeType("image/jpeg"))
        assertEquals("png", AvatarStoragePolicy.extensionForMimeType("image/png"))
        assertEquals("webp", AvatarStoragePolicy.extensionForMimeType("image/webp; charset=utf-8"))
    }

    @Test
    fun nonImageMimeTypeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AvatarStoragePolicy.extensionForMimeType("text/plain")
        }
    }

    @Test
    fun missingMimeTypeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AvatarStoragePolicy.extensionForMimeType(null)
        }
    }
}
