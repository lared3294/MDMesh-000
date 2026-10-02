package com.mdmesh.agent.provisioning

import android.app.admin.DevicePolicyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mdmesh.core.config.ServerConfigStore
import com.mdmesh.core.store.DeviceIdStore
import com.mdmesh.core.store.EnrollTokenStore
import com.mdmesh.core.sync.CheckInWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI

/** Explicit lab bootstrap. Uses the real worker and server-minted token; never prints credentials. */
@RunWith(AndroidJUnit4::class)
class AdbEnrollmentBootstrap {
    @Test
    fun enrollDeviceOwnerWithProvidedCredentials() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val server = args.getString("serverUrl")?.trim().orEmpty()
        val token = args.getString("enrollToken")?.trim().orEmpty()
        // Excluded from ordinary smoke runs unless an operator explicitly supplies credentials.
        assumeTrue("Manual bootstrap requires serverUrl and enrollToken", server.isNotEmpty() && token.isNotEmpty())
        val uri = URI(server)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && (uri.path.isNullOrEmpty() || uri.path == "/")) {
            "serverUrl must be the HTTPS origin of your MDMesh console"
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        check(dpm.isDeviceOwnerApp(context.packageName)) { "Set this agent as Device Owner first" }
        val identity = DeviceIdStore(context)
        check(identity.current().isNullOrBlank()) { "Agent already enrolled; refusing to replace its server" }

        ServerConfigStore(context).save(server)
        EnrollTokenStore(context).save(token)
        val manager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<CheckInWorker>().build()
        // Shares the agent's singleton EnrollmentManager mutex with all regular check-in workers.
        manager.enqueue(request).result.get()
        withTimeout(ENROLLMENT_TIMEOUT_MS) {
            while (identity.current().isNullOrBlank() || identity.secret().isNullOrBlank()) {
                check(manager.getWorkInfoById(request.id).get()?.state != WorkInfo.State.FAILED) {
                    "Enrollment rejected; inspect CheckInWorker logs before trying another token"
                }
                delay(POLL_INTERVAL_MS)
            }
        }
        assertTrue("Missing server-issued device credentials", !identity.secret().isNullOrBlank())
    }

    companion object {
        private const val ENROLLMENT_TIMEOUT_MS = 120_000L
        private const val POLL_INTERVAL_MS = 500L
    }
}
