# Connected-device test runner

Status: convenience runner for physical Android verification
Owner: RedactGuard + Harness

Use `scripts/test-redactguard-connected-device.sh` when a physical Android device is connected through `adb`. The runner does not replace the canonical scenario definitions in `docs/evidence/physical-two-apk.md`; it makes the common preflight and launch paths reproducible.

## 1. Inspect without changing the phone

This is the safest first command when Harness and/or RedactGuard may already be installed:

```bash
bash scripts/test-redactguard-connected-device.sh inspect \
  --device <SERIAL> \
  --host-apk <HARNESS_RELEASE_APK> \
  --app-apk <REDACTGUARD_RELEASE_APK>
```

The command is non-destructive. It reports:

- device model, Android/API and ABI;
- installed Harness and RedactGuard package/version identity;
- installed APK signer identity when `apksigner` is available;
- optional local APK version/signing identity;
- whether an in-place `adb install -r` is signer-compatible;
- whether local `versionCode` is a downgrade;
- whether the installed pair and local pair share the same signer.

A signer mismatch between an installed Play-distributed package and a locally upload-key-signed APK is expected when Play App Signing uses a different app-signing certificate. Do not uninstall or force a downgrade merely to bypass that result.

## 2. Smoke the packages already installed

When preserving downloaded Harness models/configuration matters, use:

```bash
bash scripts/test-redactguard-connected-device.sh installed-smoke \
  --device <SERIAL>
```

This mode never installs or uninstalls packages. It force-stops and launches the currently installed Harness and RedactGuard packages, verifies both application processes become observable, and writes privacy-safe evidence under:

```text
evidence/local/installed-smoke/<runId>/result.json
```

It proves only that the currently installed versions launch on that device. It does not prove that those versions equal the current local source tree, and it is not equivalent to the canonical physical two-APK E2E.

After the smoke passes, manually verify on the phone:

1. Harness has the intended model/preset ready and the exact preset revision is known.
2. RedactGuard reaches `AI locale pronta`.
3. Synthetic pasted text follows Input -> Definitions -> local analysis -> Review.
4. Findings are hidden by default; reveal/hide and `Oscura`/`Ignora` decisions work.
5. Export is gated until required decisions are complete.
6. The exported PDF is reopened independently and accepted redactions are absent/replaced while ignored synthetic text remains.

Use synthetic fixtures only. Do not place private/client document content in terminal output or evidence.

## 3. Canonical clean physical E2E

When exact local release APKs can be installed on a clean target, run:

```bash
bash scripts/test-redactguard-connected-device.sh clean-e2e \
  --device <SERIAL> \
  --host-apk <HARNESS_RELEASE_APK> \
  --app-apk <REDACTGUARD_RELEASE_APK> \
  --host-source-revision <FULL_HARNESS_SHA> \
  --preset-revision <PRESET_REVISION>
```

The runner delegates to `scripts/e2e-redactguard-device.sh` with release package identity. The canonical E2E intentionally refuses to replace pre-existing selected packages, verifies same-signer APK identity, stages Host-absent and Host-present states, records the interactive critical-journey attestations, and removes only installations owned by the run.

Use `--debug` only when explicitly validating the debug package identity. Performance-sensitive inference validation should use release artifacts.

## Current-device decision rule

If `inspect` reports:

- **installed/local signer match + non-downgrade version**: an in-place update can be attempted before functional testing;
- **installed/local signer mismatch**: do not use `adb install -r`; either update through the distribution channel that owns the installed signing identity, or use a clean dedicated target for local-release E2E;
- **local version downgrade**: rebuild with a higher test/release `versionCode`; do not rely on downgrade installation for release evidence;
- **installed Harness/RedactGuard signer mismatch**: treat it as a signing/distribution architecture issue because the shared-runtime permission is signature-protected.
