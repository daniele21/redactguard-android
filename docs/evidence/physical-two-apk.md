# Physical two-APK evidence

Status: required before Harness OMBRA cleanup
Owner: RedactGuard + Harness

This gate proves the cross-repository architecture on a physical ARM64 Android device. Emulator evidence may be useful during development but must not be used to claim this gate.

## Build identity

Record before the run:

- exact Harness commit and Host APK SHA-256;
- exact RedactGuard commit and APK SHA-256;
- `consumer-android` version resolved by RedactGuard;
- device manufacturer/model, Android version and serial/asset label;
- signer SHA-256 for both APKs.

Harness and RedactGuard must be signed by the same accepted certificate. For local debug evidence, building both apps on the same workstation with the same debug key is acceptable. Release-like evidence uses the configured shared integration/release signer; signing material must never be committed.

## Preflight

Build the Harness host from its repository and RedactGuard from this repository. Then run:

```bash
scripts/physical-two-apk-preflight.sh \
  --device <SERIAL> \
  --host-apk <HARNESS_HOST_APK> \
  --app-apk <REDACTGUARD_APK>
```

Use `--release` only with release-like APKs. The script fails on signer mismatch, install failure or package-identity mismatch and launches RedactGuard only after both packages are installed.

## Required product evidence

Use a synthetic PDF containing at least a full name, email, phone number and postal address, plus at least one repeated/near-miss value. Never use production/client PII for evidence.

1. Start with RedactGuard installed and Harness absent. Confirm the app reports Harness unavailable and analysis is disabled.
2. Install/start Harness, make the curated PII model ready, return to RedactGuard and confirm the connection becomes `Harness connesso` without reinstalling RedactGuard.
3. Import the synthetic PDF through SAF. Confirm extraction completes and the document is not copied into persistent app state.
4. Select at least four built-in PII categories and start analysis. Confirm the Host receives `document-pii-detection` through the Consumer API and RedactGuard does not expose model selection/runtime tuning.
5. Confirm multi-chunk analysis completes sequentially when the fixture exceeds one input budget. No partial findings may be exposed if one chunk fails.
6. In Review, verify values are hidden by default. Reveal at least one value, hide it again, mark one occurrence `Oscura` and another `Ignora`.
7. Exercise cancellation during an active generation. Confirm the request is cancelled, session cleanup occurs and no partial review result is surfaced.
8. Exercise host death/restart during analysis. Confirm RedactGuard reports disconnect/failure rather than silently retrying against stale execution identity.
9. Export to a new PDF. Reopen the exported file independently (outside the exporter success callback) and verify accepted synthetic PII is absent, placeholders are present and ignored text remains present.
10. Force or simulate an unwritable/failed destination and confirm partial output is deleted best-effort and success is not reported.
11. Kill the RedactGuard process and relaunch. Confirm document text, revealed values and review task are not restored from SavedState/persistent storage.

## Pass criteria

The gate passes only when all required scenarios are recorded against the exact APK/build identities above. Repository-side unit/lint/build success, SDK resolution success or emulator success alone is insufficient.

Only after this gate is green may Harness remove `apps/local-llm-console` and the temporary legacy `local-llm-console` Consumer identity. The generic Consumer fixture remains in Harness.
