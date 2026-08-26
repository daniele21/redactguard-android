# Physical two-APK evidence

Status: required before Harness legacy-consumer cleanup
Owner: RedactGuard + Harness

This gate proves the cross-repository architecture on a physical ARM64 Android device. CI, JVM tests and emulator evidence remain useful lower-level evidence but must not be reported as this physical gate.

## Evidence levels

RedactGuard exposes two different device commands because smoke and E2E prove different claims.

### Android app smoke

```bash
bash scripts/smoke-redactguard-device.sh \
  --device <SERIAL> \
  --app-apk <REDACTGUARD_APK>
```

Use `--release` only for the release package identity. Smoke requires a clean target for the selected RedactGuard package, installs the exact APK, verifies launch/process viability, force-stops it and removes only the installation owned by the run. It writes bounded identity-bearing evidence under `evidence/local/smoke/`, which is ignored by Git.

Smoke proves APK install/launch/cleanup. It does **not** prove Harness integration or the document-redaction journey.

### Physical two-APK E2E

The HCP-8 candidate uses release APKs, so the canonical runner must receive `--release`; omitting it would make the runner target the debug package identities instead of the signed release packages.

```bash
bash scripts/e2e-redactguard-device.sh \
  --device <SERIAL> \
  --host-apk <HARNESS_HOST_APK> \
  --app-apk <REDACTGUARD_APK> \
  --host-source-revision 9699cb0ae9bd6b49f68c07fa49c004360e8d7d92 \
  --preset-revision 3 \
  --release
```

Run this command from the same frozen RedactGuard checkout used to build the APK (`8ca1f50f0ca07c04bd19dbc3a870366f77f06689`). The runner records `APP_SOURCE_REVISION` from its own Git checkout, so running it from a later documentation-only revision would produce misleading source evidence even if the APK bytes came from the frozen candidate.

The E2E command is intentionally interactive because SAF selection, Review decisions, Host death/recovery and independent exported-PDF inspection require a real operator on the physical device until native automation can prove those boundaries without weakening them.

The command:

- refuses to replace pre-existing Harness or RedactGuard packages;
- verifies both APKs have the same signer before installation;
- stages RedactGuard first so Host-absent behavior is observable;
- then installs the exact Harness APK and requires the operator to make the declared preset ready through Harness-owned controls;
- treats the RedactGuard `AI locale collegata` badge only as Binder/transport connectivity; assignment, preset, capability and execution readiness are proven only when analysis starts successfully;
- records explicit checkpoint attestations for the manual critical journey;
- force-stops and removes only packages installed by this run on success/failure/interrupt;
- writes privacy-safe evidence to `evidence/local/e2e/` with APK hashes, signer, source/device/SDK/preset identity and no document content.

`physical-two-apk-preflight.sh` remains a lower-level install/launch helper for focused debugging; it is not the canonical complete E2E command.

## Exact signed APK preparation

HCP-8 freezes the last runtime-bearing source revisions that have exact-head repository/package validation, rather than later documentation-only descendants:

- Harness: `9699cb0ae9bd6b49f68c07fa49c004360e8d7d92` (`versionCode=28`, `versionName=1.0.0`);
- RedactGuard: `8ca1f50f0ca07c04bd19dbc3a870366f77f06689` (`versionCode=9`, `versionName=0.1.4`).

Build both release APKs from clean detached checkouts at those exact revisions. The helpers deliberately reuse the same existing Harness upload key so the two release package identities satisfy the signature-permission boundary without a Play round trip.

Harness:

```bash
git fetch origin
git switch --detach 9699cb0ae9bd6b49f68c07fa49c004360e8d7d92
git status --porcelain
bash scripts/build-phone-test-release.sh build-apk
```

Expected output:

```text
apps/local-llm-phone-test/build/outputs/apk/release/local-llm-phone-test-release.apk
```

`build-apk` preserves the already-selected phone candidate identity; it does not execute the separate Play/AAB version increment performed by `build`.

RedactGuard:

```bash
git fetch origin
git switch --detach 8ca1f50f0ca07c04bd19dbc3a870366f77f06689
git status --porcelain
bash scripts/build-redactguard-release.sh build-apk
```

Expected output:

```text
app/build/outputs/apk/release/app-release.apk
```

Keep this RedactGuard checkout at `8ca1f50f0ca07c04bd19dbc3a870366f77f06689` while running `scripts/e2e-redactguard-device.sh`; return to `dev` only after evidence has been written. Both build helpers fail closed on dirty source, verify the generated APK signature and print the exact source revision associated with the artifact.

For a clean Host installation with the repository seed state, the current RedactGuard PII preset is `qwen35-json` revision `3`; record it as `qwen35-json:3` unless Harness shows that the actual published Control Plane state differs. Harness remains authoritative if the preset has been explicitly changed before the run.

## Build and environment identity

A passing physical E2E records at least:

- exact Harness source revision and Host APK SHA-256;
- exact RedactGuard source revision and APK SHA-256;
- resolved `consumer-android` version;
- Harness preset revision used by the run;
- device model, Android/API/ABI identity and test-target serial/asset identity;
- signer SHA-256 for both APKs.

Harness and RedactGuard must be signed by the same accepted certificate. Signing material must never be committed or included in evidence.

## Required scenario matrix

Use synthetic fixtures only. Private/production/client documents must not be committed, attached to evidence or copied into logs.

### Input and parsing

1. **Pasted text:** synthetic text containing a full name, email, phone number and postal address reaches Definitions and the same canonical analysis path as PDF input without invoking the PDF parser.
2. **Single-page text PDF:** extraction succeeds and reaches Definitions/analysis without `RG-PDF-004` or `RG-PDF-005`.
3. **Multi-page text PDF:** page order/canonical segmentation remain stable and no truncation is silently accepted.
4. **Image-only PDF:** fails explicitly as `RG-PDF-008 / IMAGE_ONLY_PDF`; OCR/VLM is not invoked automatically.
5. **Unexpected parser failure, if reproduced:** surfaces `RG-PDF-005 / PARSER_FAILED`, not `MALFORMED_PDF`; expanded technical details expose only safe parser step/type plus operation ID.

### Local-AI lifecycle and analysis

1. Start with RedactGuard installed and Harness absent. Confirm the app reports AI-local unavailable/not-installed state and analysis cannot start.
2. Install/start Harness, make the declared PII preset ready, return to RedactGuard and confirm the product reaches **`AI locale collegata`** without reinstalling RedactGuard. This badge proves transport connectivity only; start analysis and verify the authoritative Control Plane discovery/activation/capability path succeeds before treating Local AI as ready for the task.
3. Select representative built-in PII categories and start analysis. RedactGuard must not expose model selection/runtime tuning.
4. Multi-chunk analysis completes sequentially when required. A later chunk failure exposes no partial findings.
5. Cancellation during active generation cancels the operation/session and exposes no partial review result.
6. Host death/restart during analysis produces classified disconnect/recovery rather than silently reusing stale execution identity.

### Review and export

1. Findings are hidden by default. Explicitly reveal then hide at least one value.
2. Mark at least one occurrence `Oscura` and another `Ignora`; export remains unavailable until every required decision is complete.
3. Export to a new PDF and reopen it independently of the exporter callback. Accepted synthetic PII is absent/replaced and ignored text remains present.
4. Exercise an unwritable/failed destination where practical. Partial output is cleaned best-effort and success is never reported falsely.

### Process-local privacy and cleanup

1. Starting a new document clears prior task-local input/findings/reveal/review state.
2. Kill and relaunch RedactGuard; sensitive task state is not resurrected from persistent state.
3. Diagnostic/evidence output contains stable codes/build/run/environment identity but no prompts, document text, finding values or raw Binder payloads.
4. The E2E command removes only the packages it installed and verifies the target returns to the initial package-absence state.

## Pass criteria

The gate passes only when every required applicable scenario is observed against one exact identity set and the interactive E2E command records all required attestations with verified cleanup.

A green repository build, packaged SDK resolution, successful smoke run or emulator journey is not equivalent to this gate.

Only after this physical gate is green may the corresponding Harness legacy RedactGuard/OMBRA compatibility be removed when its Harness-side workstream also permits cleanup.
