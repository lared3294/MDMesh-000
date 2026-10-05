# Comparative QR provisioning test with the original agent

The lab builds use commit `27909e473ba0c7961f2889a4fbcdfa7f659aa66b`, before local
application restrictions, inventory changes and factory-reset policy. All 198
tracked Android files were compared with that commit: no source/build-file edits.
The modified branch and server remain in place.

Two variants were built with the original Gradle build types, versionCode 16,
versionName 0.1.15, minSdk24 and targetSdk35:

| Variant | Package | Packaging |
| --- | --- | --- |
| Debug | `com.mdmesh.agent.debug` | Original debuggable build, without R8 |
| Release | `com.mdmesh.agent` | Original non-debuggable build, R8/resource shrinking |

Both are signed with the existing local Android debug certificate, including the
release variant via the original release-signing environment variables. They do
not carry the project's official release signing certificate. The certificate
SHA-256 is `8faacb8b74822f3f8638b64dded7f30fb6b11aeee892ad678585d36979b05a99`;
QR signature checksum is `j6rLi3SCLz-GOLZN3tfzD7axGu7okq1nhYXTaXmwWpk`.

Artifact SHA-256:

- Debug: `372e13c768483de9b1a6fba7adb800e7ac70f2c93e42fce3d8f3bf7a2a944727`
- Release: `e90f865205d3a8fb59c07743a84002ad24649c6f18084c6f754d0556ea56620d`

## Procedure

Host each APK under a distinct `/files/` filename on the current lab server,
verify a public unauthenticated download and its SHA-256, then use the standalone
`MDMesh-QR-original.html` helper to generate the matching QR. The helper uses the
existing server/token extras and QR options; it has no external network calls or
persistent token storage. Its bundled QR renderer is the same dependency used by
the web console. This does not require changes to `.env`, Caddy or the normal QR.

Obtain a new single-use enrollment token for each attempt from the current
console. Use a neutral lab configuration. Scan debug first, then release, each
from the Android provisioning wizard on a freshly reset lab tablet. Record the
last screen and whether it passes "preparing work setup" and completes setup.
Do not replace a version19 agent in place with these older version16 APKs: this
is a fresh-provisioning experiment, not an upgrade or downgrade of an enrolled
managed device.

## Interpretation

- Original debug succeeds where modified debug failed: evidence points toward a
  local source change, assuming comparable device/server/QR conditions.
- Original debug fails but original release succeeds: package identity and
  packaging/debuggable differ; this narrows the search without isolating one.
- Both original local builds fail: local signing, identity, build environment,
  DPC approval or provisioning conditions remain candidates. It does not prove
  a signing defect or rule out a source difference from the exact official APK
  that previously worked (that official release version has not been identified).
- Passing the error screen but later failing enrollment separates Android setup
  acceptance from server handshake/configuration. Capture both outcomes.

Builds/signatures/components and the offline QR helper were checked locally.
No hardware QR enrollment was executed here, and no production wipe/deploy was
performed. Further feature work is deferred at the user's request.
