package io.github.by4gnusd3i.familyplanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidManifestTest {
    @Test
    fun appDoesNotRequestInternetPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )

        assertFalse(packageInfo.requestedPermissions.orEmpty().contains(Manifest.permission.INTERNET))
    }
}
