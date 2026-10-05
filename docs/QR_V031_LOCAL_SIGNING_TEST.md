# QR provisioning: v0.3.1 release with the local certificate

The user confirmed that the unmodified official APK downloaded from
`https://mdm.asse.uy/files/agent.apk` completes QR provisioning and appears in the
modified MDMesh server. The pristine baseline debug and release builds with the
local certificate both failed while preparing work setup.

This next experiment rebuilds the official `v0.3.1` tag in release mode, with
versionName `0.3.1`, versionCode `301` and the existing local certificate. The
remote tag resolves to commit `55d09078def99bf6c31b92014024a93790cc5eb1`, matching
the local tag. All 197 tracked Android files in the isolated build copy were
compared to the tag with no changes. Only documented signing environment
variables and Gradle version/base-URL properties are supplied at build time.

A source comparison with baseline `27909e4` found no Android runtime-source
changes; the Android differences were README/static-analysis configuration.
The baseline APK's default `0.1.15 / 16` metadata must therefore not be presented
as evidence of different provisioning callback source code from official 0.3.1.

## Reproduction

Archive the tag into an isolated directory. With JDK17, the committed Gradle
wrapper, Android SDK35 and the existing local signing keystore, supply
`MDM_RELEASE_STORE_FILE`, `MDM_RELEASE_STORE_PASSWORD`, `MDM_RELEASE_KEY_ALIAS`
and `MDM_RELEASE_KEY_PASSWORD` through the environment, and run:

```sh
./gradlew :app:assembleRelease -PmdmBaseUrl=https://mdm.example.com/ \
  -PversionName=0.3.1 -PversionCode=301 --no-daemon --console=plain
```

The signing key remains local and is not included in the deliverables. The APK
is not signed with MDMesh's official release certificate. Its package is
`com.mdmesh.agent`, with minSdk24/targetSdk35, non-debuggable release build,
R8/resource shrinking and the original provisioning components.

## Hardware experiment

Serve the built APK on the modified server at the separate filename
`/files/mdmesh-v031-local-release.apk`. Verify an unauthenticated download and
its SHA-256, then open `MDMesh-QR-v031-local.html` on the workstation. Enter the
modified server origin and a freshly generated token from that same server.
The helper uses the local certificate checksum and production package, rather
than the official certificate or the `.debug` package.

Use a freshly reset lab tablet and the Android provisioning scanner. Do not
try to replace the officially signed enrolled agent in place: the local signing
certificate differs. Record whether Android passes preparing work setup,
finishes setup, and the device appears in the console.

Failure strengthens the local signing/identity/DPC-approval hypothesis but is
not proof of the specific platform rejection. Success points to differences in
the previous build metadata/environment/QR. Neither outcome should be presented
as a guarantee that all modified-agent policies can provision successfully.

## Verified build and comparison

The release build completed; APK SHA-256 is
`316244934c008da1a44705b7be3e53ca9d8749476d31acff3563b7c13f18aeea`.
`apksigner verify` confirms the existing local certificate SHA-256
`8faacb8b74822f3f8638b64dded7f30fb6b11aeee892ad678585d36979b05a99`,
QR checksum `j6rLi3SCLz-GOLZN3tfzD7axGu7okq1nhYXTaXmwWpk`.
Production package/version/SDK and provisioning manifest components were checked.

Comparing the uncompressed contents of the known-working official binary and
the local build, excluding only signature files (`META-INF/MANIFEST.MF`,
`.SF/.RSA/.DSA/.EC`) and the APK signing block, gives **386 identical files of
387**, with no added/missing entries. DEX, AndroidManifest.xml and resources
are byte-identical. The sole differing entry is `kotlin-tooling-metadata.json`:
`buildSystemVersion` is `8.10` in the official binary and `8.10.2` locally.
Other fields in that metadata file match. APK signatures intentionally differ.

`MDMesh-v031-local-release.apk`, `MDMesh-QR-v031-local.html` and a Spanish guide
are delivered outside the repo. Chromium checked the QR component, local
certificate checksum, download URL, modified-server/token extras and canvas,
with no external requests. Hardware enrollment remains pending. No device
reset, remote deployment, official-key access or production-code edit occurred.
