# Local AI connected-device diagnostics

Status: support workflow for installed Harness + RedactGuard failures
Owner: RedactGuard Local AI consumer integration

Use `scripts/diagnose-redactguard-local-ai-device.sh` when RedactGuard can connect to the installed Harness but analysis fails before inference, especially around the consumer control plane (`control-plane.assigned-use-cases`, preset discovery, activation, or compatibility).

The workflow is diagnostic evidence, not a replacement for `docs/evidence/physical-two-apk.md` or the canonical clean physical E2E.

## Snapshot

```bash
bash scripts/diagnose-redactguard-local-ai-device.sh snapshot \
  --device <SERIAL>
```

The snapshot checks the installed package/version identity, exact installed APK SHA-256, package UID, the signature-gated shared-runtime permission, Harness service declaration, target process state, and the ActivityManager Binder-service state. Service identity matching accepts the fully qualified Android class name and the package-relative/component forms emitted by current Android `dumpsys` output, so formatting differences are not treated as product failures. UID discovery similarly accepts both `userId`/`appId` package fields and falls back to the package-manager UID listing; if none is exposed, it reports `unavailable` rather than a blank value.

If the installed Harness build permits `run-as`, the snapshot also copies the private `harness-control-plane.db` plus WAL sidecars and reports only technical control-plane rows for:

- application `redactguard`;
- use case `document-pii-detection`;
- assignment/binding revisions;
- preset revisions;
- preset exposures/default selection.

When `run-as` is unavailable, direct private-DB inspection is explicitly reported as `N/A`; the script does not treat inaccessible private state as a passing check.

## Reproduce the failure

For the current `RG-AI-012` / `control-plane.assigned-use-cases` investigation, prefer:

```bash
bash scripts/diagnose-redactguard-local-ai-device.sh reproduce \
  --device <SERIAL> \
  --restart
```

`--restart` force-stops and relaunches the two apps without clearing data. This is useful because Harness control-plane reconciliation runs when the installed Harness runtime graph is created. After launch, the script asks the operator to trigger the failing RedactGuard analysis (or tap `Riprova`) and then captures only technical lines from the two target process logs plus the post-reproduction Binder-service state.

The script intentionally does **not** use `pm clear`, uninstall either app, dump the UI hierarchy, capture document text, prompts, generated output, or Binder payloads. Use synthetic/non-sensitive input whenever possible.

Evidence is written locally under:

```text
evidence/local/local-ai-diagnostics/<runId>/
```

The useful files are:

- `report.txt` — complete command summary and PASS/WARN/FAIL diagnostics;
- `technical-log.txt` — filtered target-process technical lines from the reproduction window;
- `service-state.txt` — filtered ActivityManager service/binding state;
- `control-plane.txt` — direct persisted control-plane evidence when `run-as` is available, otherwise the explicit `N/A` reason.

## Interpretation

A healthy package/signing/service snapshot proves that RedactGuard can legitimately bind to the expected Harness service; it does not by itself prove that `assignedUseCases()` can complete. A healthy direct control-plane snapshot additionally proves that the expected persisted application/use-case/binding/preset state exists. The `reproduce` mode is the authoritative diagnostic path for the actual Consumer SDK call because the signature-gated `assignedUseCases()` Binder transaction cannot be invoked directly from a generic `adb shell` process.

If the package/signing/service checks pass, the control-plane rows are present (or inaccessible), the Binder service remains bound, and the same `RuntimeException` still occurs at `control-plane.assigned-use-cases`, investigate the Consumer SDK/Host Binder protocol boundary rather than model loading or inference.
