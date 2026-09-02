# Current state

Status: active
Document type: current-state
Owner: redactguard-android
Canonical scope: repository.current-state
Last reviewed: 2026-09-02

## Integrated product state

RedactGuard is a standalone Android document-protection product consuming the Harnex Consumer Android SDK over Binder; Harnex retains model/runtime/GGUF/residency ownership.

Implemented capabilities on `dev` include PDF/pasted-text ingestion, built-in/custom PII selection, Consumer SDK `0.1.0-alpha.6` task metadata, runtime readiness, bounded sequential analysis, atomic validation, privacy-safe diagnostics, masked review, fail-closed redaction/export, adaptive Review, SAF PDF export and process-local sensitive state. OCR/VLM and cloud fallback remain out of scope.

## Engineering baseline

`dev` carries `repo-template-sw` 0.8.0 with repository-owned Android/product-UI customizations preserved. `.engineering/*`, local skills and CI own the operating contract.

## Mobile product experience

`dev` contains the target-derived visual system and graphics, process-local summary projection, corrected Document/Analysis/Protection/Review/Outcome/Recovery surfaces, hidden-by-default Review values, selected-profile evidence, landscape expanded Review, target-comparison Visual Evidence v2 and 14-checkpoint E2E evidence.

The approved visual identity remains anchored by SHA-256 `21b55331634fb0aafeeafdef971d8b43489f5eedbda30bc21e3fdade92371b5a`; `design/reference/approved-target.png` and `target-provenance.json` retain equivalence evidence. Launcher identity uses legacy + API26 adaptive resources. Persisted History, fabricated progress/metrics, OCR/VLM, exact PDF-coordinate preview, cloud fallback and fake Share remain excluded. Top-level `Analizza / AI locale / Impostazioni` navigation and the owned read-only Local AI setup/readiness surface are implemented on the active LAS candidate but are not yet integrated on `dev`.

## Automated evidence

VUI-18 is integrated on `dev` as merge `583f7a58dcbd55be5611d1f7125e7e90d4f38c76`. Its validated head `4536c302d7d528b01045499e3cf2a10d422f643d` passed FULL remote preflight `33271498163`, Emulator E2E v2 `33271488297` and Visual Evidence v2 `33271488301`.

The exhaustive `CHUNK_FAILED` regression suite is validated on exact head `6aec3906f206421dde01bd5694f44eb2b0841efb`: Validate `33399004284`, Emulator E2E `33399004372`, Repository health `33399004482`, Visual evidence `33399004674` and Two-APK emulator E2E `33399005355` passed.

Failure-diagnostics hardening PR #142 is merged on `dev` as `6627d082ed1f1b8b63b1759662a1b05b0138e5e2`. Its exact feature head `0157d4f206da763e556b57474459f4ded23fc818` passed Validate `33405404519`, Repository health `33405404520`, Emulator E2E `33405404486`, Visual evidence `33405404542` and Two-APK emulator E2E `33405406165`. Post-merge exact-commit Validate `33407219310`, Repository health `33407219235`, Package RedactGuard Artifacts `33407219340` and Publish Play Internal `33407219202` also passed; the signed release AAB was verified and published to Google Play Internal Testing.

## Active Local AI blocker

Earlier physical installed-pair evidence proved same signer, granted Local AI permission and a live RedactGuard-to-Harnex Binder service while `RG-AI-002 / HOST_UNAVAILABLE` reproduced. A later physical reproduction advanced into analysis and surfaced `RG-AI-008 / CHUNK_FAILED`; representative ARM64/JNI/GGUF evidence for that path remains separately required.

The active LAS candidate PR #143 is currently at `0c7aa5dd70043c95db7aca9c5c1e3035300b09ff`. It includes the `Analizza / AI locale / Impostazioni` navigation, Local AI readiness/recovery UX, durable logical-job consumption and Consumer SDK `0.1.0-alpha.9`; Harnex `dev` is `f86b53ad29d2396660f095d5eaadd41c19bda8c7`, including PR #511's concrete Binder setup-resolution forwarding.

Exact Two-APK emulator run `33594812860` on that LAS head failed before the lifecycle continuation scenarios. The Host artifact build and Host-absent fail-closed scenario passed; same-signer installation and Binder negotiation reached protocol minor 6 with `consumer-setup-resolution-v1`; assigned use-case and published-preset discovery both validated. RedactGuard then remained at generic `INCOMPATIBLE` / `Configurazione AI non disponibile`, and diagnostics stopped after `control-plane.published-presets result=VALIDATED` without a setup-resolution event.

Source inspection identifies a RedactGuard diagnostics masking hazard: setup rejection formatting can violate the technical-event reason-token contract and throw before the original typed `ConsumerControlPlaneFailure` is recorded. Therefore the underlying functional setup rejection is **not yet attributed to Harnex or RedactGuard**. The current executable LAS slice is to make setup diagnostics non-interfering, preserve the typed Consumer failure code, and rerun the exact Two-APK setup path before any speculative model/runtime/configuration patch. See `docs/workstreams/local-ai-setup-readiness.md`.

## Remaining evidence

1. Run VUI-7 physical accessibility/adaptive evidence on a named Android device.
2. Run the same-signer Harnex + RedactGuard ARM64 journey, including the current `RG-AI-008` reproduction, and capture stable code/stage/operation plus safe typed Consumer identity and boundary step.
3. Verify live GitHub branch/default-branch/required-check protection; desired policy alone is not enforcement evidence.
4. Complete LAS-09 diagnostics non-interference, classify the typed setup-resolution result through a fresh exact Two-APK run, fix only the confirmed canonical owner, then finish readiness semantics/UX consistency and lifecycle evidence.
5. After deterministic lifecycle evidence is green, capture the separate representative ARM64/JNI/GGUF/OEM/model-residency evidence required for physical-runtime claims.

Active workstreams: `android-visual-reference-convergence.md`, `document-ingestion-v2.md`, `failure-diagnostics-hardening.md`, `ombra-to-redactguard-migration.md`, `harness-control-plane-consumer-cutover.md`, `local-ai-setup-readiness.md`.

## Current boundary

Do not add OCR/VLM, cloud parsing, model selection/configuration, llama.cpp ownership or Harnex administration to RedactGuard through these workstreams. LAS may expose only versioned consumer-safe read-only setup/runtime state published by Harnex; RedactGuard must never reconstruct or mutate Harnex-owned model/configuration state. A generic product `INCOMPATIBLE` state is not sufficient evidence to assign a Harnex functional bug.