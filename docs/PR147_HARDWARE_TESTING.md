# PR #147: hardware testing

Prepared on 2026-11-09. Branch: `testing/pr147-hardware`, based on upstream main `5b25bd3`.
The feature commit `97601f0` was reapplied as `645af63`; conflicts preserve main's wiped-device guards and
Sync now connection response. This branch includes main's Users & roles management.
The original draft PR has not been rewritten: hardware verification is still pending.

## Installed environment

- Console: https://mdm.testing.asse.uy
- Server/console: `0.4.0-pr147.20261109`.
- Testing APK: https://mdm.testing.asse.uy/files/agent-pr147-testing.apk
- APK: `com.mdmesh.agent.debug`, version `0.4.0-pr147.cfd787d`, versionCode `401`.
- SHA-256: `0921dc8839a3e02408e069e1f56fe776bdb7af12fccc9b3d9429152149f34004`.
- Signing certificate SHA-256: `8faacb8b74822f3f8638b64dded7f30fb6b11aeee892ad678585d36979b05a99`.
- Its fallback server URL is testing, set by `scripts/testing/pr147.gradle`. A stored enrollment URL takes precedence.
- The official `/files/agent.apk` and existing enrollment QR remain unchanged. That QR does not install this test APK.
- No agent rollout or hardware action was initiated by this deployment.

## Dedicated device setup

Use a dedicated test device without another Device Owner. The debug package is different from the official
`com.mdmesh.agent`; it does not upgrade that package. Existing debug installations need the same certificate.
Do not clear or replace a production device's Device Owner for this test.

With USB debugging enabled and no accounts/other owner preventing Device Owner enrollment:

```bash
adb devices
adb install -r agent-pr147-testing.apk
adb shell dpm set-device-owner com.mdmesh.agent.debug/com.mdmesh.agent.admin.AdminReceiver
adb shell am start -n com.mdmesh.agent.debug/com.mdmesh.agent.MainActivity
adb shell dumpsys device_policy
```

Complete enrollment in the app against testing. Confirm its device entry appears in the testing console, its
reported agent version is `0.4.0-pr147.cfd787d`, and capabilities include `app.silentUninstall` and `app.setHidden`.
Use a dedicated configuration without automatic Install assignments for the disposable test app, so reconciliation
does not reinstall it during the uninstall test.

## Hardware acceptance checklist

Record Android version, manufacturer/model, device number, time, command IDs and results for each case.
Use a disposable user app and seed some local data before starting.

1. Open Device → Apps → Scan now. Confirm a full scan, including non-launcher packages and hidden apps.
2. Cancel Disable and Uninstall confirmations. Confirm no command was queued.
3. Disable the disposable app. Confirm `app.setHidden` finishes successfully, the app disappears from the launcher,
   the full inventory still lists it as hidden, and the inventory refreshes automatically.
4. Enable that app. Confirm it returns to the launcher and its local data is retained.
5. Disable it again, then uninstall it while hidden. Confirm `app.uninstall` succeeds and the next full scan removes
   the package. Reinstall it independently if more tests are needed.
6. Confirm uninstall of a system app and hiding/removal of the agent, current launcher/keyboard, active admins,
   essential components and active kiosk packages are refused. Do not attempt to bypass guards on essential apps.
7. Confirm successful install, uninstall and hide/unhide refresh inventory; failures never imply a successful change.
8. Disconnect the test device, queue a disposable-app action and reconnect it. Check delivery/result sync and final
   inventory. Do not treat an offline queued action as applied.
9. Check Observer access: inventory/export works, mutation controls are absent. An old agent shows an update prompt
   and cannot use the new actions. Wiped devices have no app action controls.
10. Reboot the dedicated device, check in and rescan. Confirm management and restored app availability persist.

Hardware results remain PENDING until these cases are executed on a physical Device Owner device.

## Validation before deployment

- Common/contract: 286 tests, zero failures.
- Android: 15 proto + 189 core tests; Detekt passed; APK assembled.
- Web: 602 tests across 35 files, all passed outside the sandbox; TypeScript/Vite build passed.
- Server: full reactor WAR packaging passed.

## Operations and rollback

Active stack: `/root/mdmesh-production-clone-20261104`, Compose project `mdmesh-prodclone`.
Artifacts/evidence: `/root/mdmesh-pr147-20261109`.
Backup: `/root/mdmesh-backups/pre-pr147-20261109` (dump, persistent files, Compose/environment and checksums).
Before deployment: 14 devices, 5 configurations, 3 users, 4 roles.

Main adds nullable `devices.wipedAt`; the PR's app-management changes add no database migration.
Software rollback preserves current data and leaves this additive column in place:

```bash
bash /root/mdmesh-pr147-20261109/rollback.sh
```

The dump is a separate recovery point; the rollback script does not restore it or discard subsequent writes.
Automatic updates and supervisor apply remain disabled for this custom testing build.
