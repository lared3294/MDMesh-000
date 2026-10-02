package com.mdmesh.policy

import android.os.Build
import com.mdmesh.policy.wifi.DpmHandle

/** Restricts the user's Settings reset, while leaving the Device Owner's remote wipe available. */
class FactoryResetPolicy(private val handle: DpmHandle) : TogglePolicy {
    override val capabilityKey: String = CAPABILITY_KEY

    override fun isSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            handle.dpm.isDeviceOwnerApp(handle.admin.packageName)

    override fun setEnabled(enabled: Boolean): PolicyOutcome = runCatching {
        UserRestrictions.forKey(CAPABILITY_KEY).orEmpty().forEach { restriction ->
            if (enabled) handle.dpm.clearUserRestriction(handle.admin, restriction)
            else handle.dpm.addUserRestriction(handle.admin, restriction)
        }
        PolicyOutcome.Applied
    }.getOrElse { PolicyOutcome.Failed(it.message ?: "factoryReset setEnabled failed") }

    companion object {
        const val CAPABILITY_KEY = "factoryReset"
    }
}
