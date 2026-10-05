# QR enrollment: Android 16 signing comparison and Android 8.1 / 13 success

## Environment and outcome

Samsung SM-X236B, Android 16. The current modified MDMesh server completes QR
provisioning with the unmodified official MDMesh APK downloaded from
`https://mdm.asse.uy/files/agent.apk` (0.3.1 / 301, `com.mdmesh.agent`). The tablet
appears in the console.

The user reports a generic error at "preparing work setup" with all tested local
builds, including a pristine v0.3.1 release build with versionName0.3.1/code301,
production package and the local certificate. Local debug, non-debuggable probe,
and pristine baseline release builds previously failed at the same stage.

Normal ADB installation and application launch work. Setting Device Owner via
ADB works; both provisioning callback smoke tests passed on the Samsung. The
explicit ADB enrollment helper completed server registration, and application
restrictions and remote installation were tested successfully. The Settings
factory-reset restriction also worked on the modified agent.

## Additional hardware result (2026-10-05)

The user confirmed that the modified agent build19 (`MDMesh-politicas-v19.apk`,
versionName0.1.15/versionCode19, `com.mdmesh.agent.debug`, local signing
certificate) completes QR enrollment from a factory-reset Samsung SM-P585M
running Android 8.1.0. The user also confirmed that the restrictions work on
this device. This is user-reported hardware acceptance, not a locally observed
or exhaustive test of all policies, remote operations and reboot persistence.

The earlier report listed `factoryReset: unsupported`, with `screenshots`,
`usbStorage`, `userAppInstall`, `userAppUninstall` and `location` as `applied`.
Build17 was an earlier provisioning probe, predating the full application
inventory (build18) and Settings factory-reset implementation (build19).
The follow-up with build19 establishes successful enrollment and restrictions
on Android 8.1; the earlier result does not establish an Android 8.1 limitation.

The user additionally reports successful QR enrollment and working restrictions
on a Samsung SM-A035M running Android 13, in the ongoing build19 test procedure.
The APK hash was not independently captured for this device. As above, this is
user-reported acceptance, not an exhaustive test of each policy and remote action.

| Device | Android | QR with local agent | Restrictions |
| --- | --- | --- | --- |
| Samsung SM-P585M | 8.1.0 | Works (build19 confirmed) | Work, user-reported |
| Samsung SM-A035M | 13 | Works, user-reported | Work, user-reported |
| Samsung SM-X236B | 16 | Fails with tested local builds; official APK works | Work after ADB enrollment |

These successes contrast with the SM-X236B / Android 16 local-build failures above.
The observed QR failure is not universal across the tested Samsung devices.
OS version, model, firmware and provisioning services differ; these three devices
alone do not isolate Android version as the cause or identify a version cutoff.
Local signing works for QR on the Android 8.1 device, and the ongoing Android 13
test also succeeds, while the controlled
Android 16 comparison still suggests an admission check associated with signing
identity. The exact rejecting component remains unconfirmed.

## Controlled comparison

Official v0.3.1 source tag resolves to
`55d09078def99bf6c31b92014024a93790cc5eb1`, checked against GitHub. All197 tracked
Android files in the isolated build copy match the tag. Release/R8 build with
original Gradle settings and documented version/baseURL/signing properties.

| Property | Official, succeeds | Locally compiled, fails |
| --- | --- | --- |
| Package | com.mdmesh.agent | com.mdmesh.agent |
| Version | 0.3.1 / 301 | 0.3.1 / 301 |
| SDK | min24, target35 | min24, target35 |
| APK SHA256 | 78c1d05bd7e80c27769bd73804f6736436847128929afb95a35d073d14885aba | 316244934c008da1a44705b7be3e53ca9d8749476d31acff3563b7c13f18aeea |
| Cert SHA256 | 3f4b357285e895f0f0bd90fe5fa0cd1d530cdad55a48cf6888097d508302c5fc | 8faacb8b74822f3f8638b64dded7f30fb6b11aeee892ad678585d36979b05a99 |
| Cert DN | CN=MDMesh,O=MDMesh,C=US | C=US,O=Android,CN=Android Debug |
| Key | RSA4096 | RSA2048 |
| APK signing | v2 verified, v1/v3/v3.1/v4 absent | v2 verified, v1/v3/v3.1/v4 absent |

The ZIP contents, excluding only signature records, contain387 entries:386 are
byte-identical with no added/missing files. DEX, AndroidManifest.xml, resources
and remaining metadata match. The sole differing non-signature entry is
kotlin-tooling-metadata.json: Gradle8.10 official vs8.10.2 local. Other fields
in that JSON match. This is not a comparison of whole APK bytes or compression
layout; signatures and their blocks intentionally differ.

The locally generated QR uses the correct certificate checksum for each APK,
production package/admin component, a separate URL on the modified server,
and fresh server enrollment tokens. Helper payloads were inspected in Chromium,
including certificate, URL and server/token extras. Actual remote downloaded
bytes were to be verified by the user's procedure; no server log or capture of
the last attempted download is available in this workspace.

## Assessment and missing evidence

The leading hypothesis is provisioning admission associated with signing
identity, including Android Enterprise DPC approval/Play Protect, rather than
our restriction implementation or an invalid APK signature. Both signatures
verify and use the same signature scheme. Certificate key sizes differ, but
this alone does not identify a failing platform rule.

Google states that DPCs must be verified and approved for enterprise enrollment:
https://support.google.com/work/android/answer/16694822?hl=en
https://developers.google.com/android/play-protect/warning-dev-guidance#android-enterprise-dpc-enrollment

The generic Samsung error does not confirm that this specific approval check
failed. No wizard log identifies the rejecting component. The approval status
of the MDMesh certificate and any approval association with package/certificate
have not been independently confirmed. Passing ADB callbacks does not test
setup-wizard admission controls. General developer account verification must
not be conflated with DPC approval.

## Questions for maintainers / Android Enterprise support

1. Is the official MDMesh DPC signing identity approved for QR provisioning?
2. Are separately signed self-hosted builds supported, and what is the documented
   DPC approval route for such a fork?
3. Can they identify the expected Samsung/Google admission error from these
   paired outcomes or provide a supported method to collect provisioning logs?
4. Could the changes be accepted upstream and distributed through an official
   signed release if independently signed provisioning is unsupported?

This report contains no enrollment tokens, private keys or device credentials.
It is prepared locally for review only; no issue, support request, APK submission
or message has been published or sent to third parties.
