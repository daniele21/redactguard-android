# Harness Control Plane consumer cutover

Status: active
Owner: redactguard-android
Read when: adapting RedactGuard to Harness-managed presets, assigned use cases, activation lifecycle or removing consumer-side binding assumptions
Last reviewed: 2026-08-31

## Goal

Keep RedactGuard a pure Consumer SDK client while replacing consumer-side assumptions with Harness-owned discovery/activation/runtime-readiness contracts. User-facing choices may select only host-published consumer-safe options; RedactGuard never owns concrete model/runtime configuration.

Canonical Harness owner: `daniele21/android-local-llm-harness/docs/shared-runtime/control-plane/roadmap.md`.

## Invariants

- Harness owns application registration, use-case/preset publication, model/config binding, activation, runtime preparation, residency and Host telemetry/decisions.
- RedactGuard owns only consumer-safe selection/lifecycle/readiness state exposed by the published SDK. A later additive SDK may also publish a read-only consumer-safe effective execution projection; that does not transfer model/configuration ownership.
- External analysis activates the exact Host-owned use-case/binding/preset revision before requesting inference capabilities.
- One activation spans the complete sequential document analysis and is released on success, failure, cancellation or close.
- Multiple valid published presets are supported; stale/withdrawn selection fails closed and is never mapped locally to a model.
- `AI locale collegata` proves Binder connectivity only; assignment/preset/configuration/runtime readiness is verified from the published Control Plane and consumer-safe readiness boundary.
- Consumer runtime phases may expose safe state such as preparing/ready/generating/failed. Under the separate LAS workstream, a versioned SDK may additionally expose the consumer-safe resolved model identity and effective generation configuration required to verify analysis setup; model paths, digests, prompts, outputs, raw Host telemetry and editable Harness runtime administration remain forbidden.
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
| RG-HCP-8 | Cross-repository physical two-APK validation | RG-HCP-7 + frozen Harness/RedactGuard candidates | ACTIVE |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Completed integration summary

RedactGuard consumes `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.6` and uses the published Control Plane plus consumer-safe runtime-readiness contract instead of reconstructing Host policy or runtime state locally.

Authoritative analysis order:

1. discover assigned `document-pii-detection` use case;
2. discover published presets and binding revision;
3. select only an advertised preset;
4. activate exact use-case, binding and preset revisions;
5. observe the source-backed consumer-safe configuration/runtime readiness contract;
6. request Consumer capabilities and open the strict JSON-schema/stateless session, allowing Harness to prepare/load/reuse/switch the exact activated model through the existing runtime owner;
7. keep activation across all sequential chunks;
8. close session and deactivate on terminal cleanup.

Integrated behavior also covers multiple published presets, process-local selection, withdrawn/stale replacement, progressive selector disclosure, Control Plane/data-plane preset consistency, source-backed preparing/ready/generating/failed projection, failure projection, cancellation cleanup and removal of the old Consumer default-preset fallback. The completed HCP integration does not publish model IDs, digests, quantization, paths or runtime tuning to product UI. `local-ai-setup-readiness.md` is the canonical follow-up for a deliberately narrower, read-only consumer-safe effective execution projection; until that SDK contract lands, RedactGuard must not reconstruct or guess those fields.

Key integrated evidence includes:

- RG-HCP-4/5/6: PR #77, exact head `b0d9d81d761ffaca411fdc826f1206156a1e6c5a`, merged `419a0a9e89fbdd6385396444e4de02993cd436cc`;
- RG-HCP-2: PR #82, exact head `075ee82522c048de052d66f46b142b0d9bcb134e`, merged `860792986537716a1c3f625a5fe6dc132a48ef0c`;
- RG-HCP-3: PR #85, exact head `9fdd9ecee3d913ecc39dd3991409928d423d1cff`, merged `53e9a5c70aaf9e69ffe936ee33cf79a07c0c045f`;
- RG-HCP-7: PR #89, exact head `7c9c121a6737d1b454a02f7bf17656f0d12fc923`, merged `be3ee4ce30e796ab282a7abdac4cc386e8dadb53`;
- source-backed Consumer runtime readiness: PR #101, exact head `b4c3c04f4f8d59c190c90ef442cc099c69924f16`, merged `af1d4c8c842f64f92c5e7332d204494ce4a2bde4`;
- repository-owned candidate packaging: PR #104, merged `e56a62e6737c7355b6e61752f3723cf590fe9ac8`;
- converged RedactGuard v11 candidate: PR #105, exact head `5be870a194fbeb37e80469fe7eb2047e31ebb3cb`, merged `4679c23a9a22e5242761fe52af97f4eb7432aec7`.

## Active validation slice — RG-HCP-8 / CRV-110

Repository-side automated preparation is complete. The remaining acceptance gate is execution on a real ARM64 Android device with same-signer release APKs and a real GGUF model.

Frozen physical source identities:

- RedactGuard: `4679c23a9a22e5242761fe52af97f4eb7432aec7`, `versionCode=11`, `versionName=0.1.4`;
- Harness: `a30f67b21e24adc6efea838e9a9d65cc78446f28`, `versionCode=31`, `versionName=1.0.0`;
- Consumer SDK: `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.6`;
- clean Host Control Plane seed: `document-pii-detection` with PII preset `qwen35-json` revision `3` when that is the source-backed published state shown by Harness.

CRV-100 automated candidate evidence is exact-source and green:

- Harness Package Android Artifacts run `33159622580` has `packageBuild=PASS`, `androidPackaging=PASS`, `sourceRevision=a30f67b21e24adc6efea838e9a9d65cc78446f28`; the packaged phone debug APK SHA-256 is `73b0a7c9a3671f168b6b0707372a1196b90132f727587b46c99a8199163023c9`;
- RedactGuard Package RedactGuard Artifacts run `33161250690` verified `sourceRevision=4679c23a9a22e5242761fe52af97f4eb7432aec7`, `sourceDirty=false`, `versionCode=11`; debug APK SHA-256 is `23a7d029f063f6d990683b068ce2ccedca1eba90344aa7c3e218843061c6cb1f`, minified unsigned release-ci APK SHA-256 is `7494948bb3f707e1682923aace289d44b9d726f6314d88e3865a2c638e8f738f`.

These CI artifacts prove deterministic source/package lineage; they are not substitutes for the signed physical inputs. Build both release APKs from clean detached checkouts at the frozen revisions using the repository-owned same-signer helpers. Run `scripts/e2e-redactguard-device.sh` from the same frozen RedactGuard checkout and pass `--release`, Harness source revision `a30f67b21e24adc6efea838e9a9d65cc78446f28` and the actual source-backed preset revision shown by Harness (expected seed value `3` for `qwen35-json`). The runner records its own checkout as `APP_SOURCE_REVISION`.

RG-HCP-8 / CRV-110 must prove same-signer authorization, Control Plane discovery/activation, automatic runtime preparation without manual model loading, source-backed readiness transitions, preset behavior including stale/withdrawn cases, missing binding, Host restart/recovery, cancellation, complete multi-chunk analysis, Review/export and classified failure evidence without document/prompt content.

## Completion

Durable destinations remain `docs/features/local-ai-consumer.md`, `docs/features/local-ai-runtime-adapter.md`, `design/ux-contract.json`, focused tests and exact physical evidence. Delete this workstream only after the complete cutover and durable handoff.
