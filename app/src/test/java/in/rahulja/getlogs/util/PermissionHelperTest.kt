package `in`.rahulja.getlogs.util

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PermissionHelperTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = application

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun getRequiredPermissions_Api33_IncludesPostNotifications() {
        val permissions = PermissionHelper.getRequiredPermissions()
        assertTrue(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun getRequiredPermissions_Api31_DoesNotIncludePostNotifications() {
        val permissions = PermissionHelper.getRequiredPermissions()
        assertTrue(!permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Test
    fun hasAllRequiredPermissions_Granted_ReturnsTrue() {
        val shadowApp = shadowOf(application)
        PermissionHelper.getRequiredPermissions().forEach { permission ->
            shadowApp.grantPermissions(permission)
        }

        assertTrue(PermissionHelper.hasAllRequiredPermissions(context))
    }

    @Test
    fun hasAllRequiredPermissions_NotGranted_ReturnsFalse() {
        val shadowApp = shadowOf(application)
        shadowApp.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        assertEquals(false, PermissionHelper.hasAllRequiredPermissions(context))
    }

    @Test
    fun permissionHelperNotNull() {
        assertNotNull(PermissionHelper)
    }
}
