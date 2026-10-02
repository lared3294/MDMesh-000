package com.mdmesh.agent.provisioning

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.PersistableBundle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** On-device checks for the QR callbacks, without a real token or server change. */
@RunWith(AndroidJUnit4::class)
class ProvisioningSmokeTest {
    @Test
    fun provisioningModeReturnsFullyManagedAndPreservesExtras() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val extras = PersistableBundle().apply { putString("smoke_probe", "preserved") }
        val intent = Intent(context, GetProvisioningModeActivity::class.java).apply {
            action = DevicePolicyManager.ACTION_GET_PROVISIONING_MODE
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, extras)
        }
        ActivityScenario.launchActivityForResult<GetProvisioningModeActivity>(intent).use { scenario ->
            val result = scenario.result
            assertEquals(Activity.RESULT_OK, result.resultCode)
            assertEquals(
                DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE,
                result.resultData.getIntExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, -1),
            )
            @Suppress("DEPRECATION")
            val returned = result.resultData.getParcelableExtra<PersistableBundle>(
                DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
            )
            assertNotNull(returned)
            assertEquals("preserved", returned?.getString("smoke_probe"))
        }
    }

    @Test
    fun complianceReturnsOkWithoutEnrollmentCredentials() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        assertTrue("Run on a lab tablet with this agent as Device Owner", dpm.isDeviceOwnerApp(context.packageName))
        val intent = Intent(context, AdminPolicyComplianceActivity::class.java).apply {
            action = DevicePolicyManager.ACTION_ADMIN_POLICY_COMPLIANCE
        }
        ActivityScenario.launchActivityForResult<AdminPolicyComplianceActivity>(intent).use { scenario ->
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
        }
    }
}
