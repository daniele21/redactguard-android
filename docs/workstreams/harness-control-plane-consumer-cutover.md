# Harness Control Plane consumer cutover

Status: active
Owner: redactguard-android
Read when: adapting RedactGuard to Harness-managed presets, assigned use cases, activation lifecycle or removing consumer-side binding assumptions
Last reviewed: 2026-08-25

## Goal

Keep RedactGuard a pure Consumer SDK client while replacing consumer-side assumptions with Harness-owned discovery/activation contracts. User-facing choices may select only host-published consumer-safe options; RedactGuard never owns concrete model/runtime configuration.

Canonical Harness owner: `daniele21/android-local-llm-harness/docs/shared-runtime/control-plane/roadmap.md`.

## Invariants

- Harness owns application registration, use-case/preset publication, model/config binding, residency and Host telemetry/decisions.
- RedactGuard owns only consumer-safe selection/lifecycle state exposed by the published SDK.
- External analysis activates the exact Host-owned use-case/binding/preset revision before requesting inference capabilities.
- One activation spans the complete sequential document analysis and is released on success, failure, cancellation or close.
- Multiple valid published presets are supported; stale/withdrawn selection fails closed and is never mapped locally to a model.
- `AI locale collegata` proves Binder connectivity only; assignment/preset/capability readiness is verified when analysis starts.
- Sensitive document state is independent of preset/control-plane state.

## Work graph

| ID | Work | Depends on | State |
| --- | --- | --- | --- |
| RG-HCP-1 | Multi-preset tolerant Consumer adapter | current Consumer API | DONE |
| RG-HCP-2 | Process-local preset state + stale-selection handling | RG-HCP-1 | DONE |
| RG-HCP-3 | Progressive preset selector UI | RG-HCP-2 + product-ui | DONE |
| RG-HCP-4 | Host-assigned use-case discovery | Harness Control Plane SDK | DONE |
| RG-HCP-5 | Activation/deactivation lifecycle | RG-HCP-4 + Harness activation API | DONE |
| RG-HCP-6 | Failure/recovery projection + truthful connected state | RG-HCP-4, RG-HCP-5 | DONE |
| RG-HCP-7 | Remove obsolete hardcoded binding assumptions | RG-HCP-2..6 | DONE |
| RG-HCP-8 | Cross-repository physical two-APK validation | RG-HCP-7 + Harness candidate | ACTIVE |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Completed integration summary

RedactGuard consumes `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.4` and uses the published Control Plane instead of reconstructing Host policy locally.

Authoritative analysis order:

1. discover assigned `document-pii-detection` use case;
2. discover published presets and binding revision;
3. select only an advertised preset;
4. activate exact use-case, binding and preset revisions;
5. request Consumer capabilities and open the strict JSON-schema/stateless session;
6. keep activation across all sequential chunks;
7. close session and deactivate on terminal cleanup.

Integrated behavior also covers multiple published presets, process-local selection, withdrawn/stale replacement, progressive selector disclosure, Control Plane/data-plane preset consistency, failure projection, cancellation cleanup and removal of the old Consumer default-preset fallback. Product UI never receives model IDs, digests, quantization or runtime tuning.

Key integrated evidence:

- RG-HCP-4/5/6: PR #77, exact head `b0d9d81d761ffaca411fdc826f1206156a1e6c5a`, merged `419a0a9e89fbdd6385396444e4de02993cd436cc`;
- RG-HCP-2: PR #82, exact head `075ee82522c048de052d66f46b142b0d9bcb134e`, merged `860792986537716a1c3f625a5fe6dc132a48ef0c`;
- RG-HCP-3: PR #85, exact head `9fdd9ecee3d913ecc39dd3991409928d423d1cff`, merged `53e9a5c70aaf9e69ffe936ee33cf79a07c0c045f`;
- RG-HCP-7: PR #89, exact head `7c9c121a6737d1b454a02f7bf17656f0d12fc923`, merged `be3ee4ce30e796ab282a7abdac4cc386e8dadb53`.

## Active validation slice — RG-HCP-8

Repository-side preparation is complete; the remaining acceptance gate is execution on a real ARM64 Android device.

Frozen physical source identities:

- RedactGuard: `8ca1f50f0ca07c04bd19dbc3a870366f77f06689`, `versionCode=9`, `versionName=0.1.4`;
- Harness: `9699cb0ae9bd6b49f68c07fa49c004360e8d7d92`, `versionCode=28`, `versionName=1.0.0`;
- Consumer SDK: `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.4`;
- clean Host Control Plane seed: `document-pii-detection` with default preset `qwen35-json` revision `3`.

The RedactGuard same-signer release-APK helper and runbook were integrated by PR #92. Its merge source `8ca1f50f0ca07c04bd19dbc3a870366f77f06689` has green push Repository health and Validate. The Harness exact-candidate release-APK helper was integrated by PR #443; runtime-bearing source `9699cb0ae9bd6b49f68c07fa49c004360e8d7d92` has green push Repository health, Validate and Package Android Artifacts. Later documentation-only descendants do not replace these frozen APK source identities.

Build both APKs from clean detached checkouts at the frozen revisions. Run `scripts/e2e-redactguard-device.sh` from the same frozen RedactGuard checkout and pass `--release`, Harness source revision `9699cb0ae9bd6b49f68c07fa49c004360e8d7d92` and preset revision `3`; the runner records its own checkout as `APP_SOURCE_REVISION`.

RG-HCP-8 must prove same-signer authorization, Control Plane discovery/activation, preset behavior including stale/withdrawn cases, missing binding, Host restart/recovery, cancellation, complete multi-chunk analysis, Review/export and classified failure evidence without document/prompt content.

## Completion

Durable destinations remain `docs/features/local-ai-consumer.md`, `docs/features/local-ai-runtime-adapter.md`, `design/ux-contract.json`, focused tests and exact physical evidence. Delete this workstream only after the complete cutover and durable handoff.
