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

## Follow-up: original official binary

The user tested both local baseline variants: same failure while preparing work
setup. On 2026-10-05 the user supplied `https://mdm.asse.uy/files/agent.apk`.
Its downloaded binary is version **0.3.1 / 301**, package `com.mdmesh.agent`,
minSdk24/targetSdk35. `apksigner verify` passes with certificate DN
`CN=MDMesh,O=MDMesh,C=US`, SHA-256
`3f4b357285e895f0f0bd90fe5fa0cd1d530cdad55a48cf6888097d508302c5fc`.
QR signature checksum: `P0s1coXolfDwvZD-X6DNHVMM2tVaSM9oiAl9UIMCxfw`.
APK SHA-256: `78c1d05bd7e80c27769bd73804f6736436847128929afb95a35d073d14885aba`.

The unmodified downloaded artifact is `MDMesh-oficial-asse.apk` outside the repo.
`MDMesh-QR-oficial.html` generates a QR for a copy served from the modified
server at `/files/mdmesh-oficial-asse.apk`, with a new token from that server.
The normal console QR and deployment environment remain unchanged. Chromium
checked the generated payload, certificate, download/server URLs, token extras
and canvas, with no external requests. Hardware result is pending.

Success would support investigating local identity/signing/approval **and** the
source-version gap: baseline 0.1.15 is not this official 0.3.1 release. It must
not be reported as proof that Google rejects the local signing certificate.
