# OMBRA to RedactGuard cross-repository migration

Status: active — repository-side complete, physical cutover pending
Document type: coordinated implementation plan
Owner: redactguard-android + android-local-llm-harness
Canonical scope: redactguard.cross-repo-migration
Started: 2026-08-17
Last reviewed: 2026-08-17

## Goal

Move the OMBRA Android product out of `daniele21/android-local-llm-harness` and establish it as the independent `daniele21/redactguard-android` application without weakening the local-only, fail-closed and two-APK security model.

The repository-side end state has been implemented: RedactGuard is independently built and consumes a versioned public Harness Consumer Android SDK rather than Harness source modules. The migration is **not** considered fully cut over until independently built Harness + RedactGuard APKs pass the physical ARM64 evidence gate and the legacy OMBRA module is then removed from Harness.

## Non-negotiable ownership

### RedactGuard owns

- Android product UI/navigation and process-local task lifecycle;
- PDF SAF import, isolated extraction, canonical segmentation and normalized PDF export;
- built-in/custom PII definitions and selection;
- structured `document-pii-detection` instruction/schema and chunk planning;
- strict model-result parsing and exact-source finding validation;
- sequential document-analysis orchestration;
- Review reveal/accept/ignore state and overlap/conflict policy;
- deterministic placeholders and replacement plan;
- synthetic PII quality corpus/policy;
- RedactGuard privacy/security evidence.

### Harness owns

- llama.cpp and model execution;
- GGUF/catalog/model lifecycle;
- memory, scheduling, admission/backpressure and telemetry;
- Binder Host service and caller authorization;
- public Consumer API/contracts and versioned Android SDK;
- host-owned use-case policy/model/preset selection;
- generic Consumer fixture and protocol/security evidence.

RedactGuard must not depend on Harness runtime/model/UI implementation modules. Harness design-system code is not a shared dependency.

## Canonical identities

| Contract | Value |
| --- | --- |
| RedactGuard release package | `io.github.daniele21.redactguard` |
| RedactGuard debug package | `io.github.daniele21.redactguard.debug` |
| Consumer logical identity | `redactguard` |
| Use case | `document-pii-detection` |
| Consumer SDK | `io.github.daniele21.localllm:consumer-android:0.1.0-alpha.1` |
| Public Maven channel | Harness `consumer-sdk-maven` branch |
| Harness release package | `io.github.daniele21.localllm.phonetest` |
| Harness debug package | `io.github.daniele21.localllm.phonetest.debug` |
| Harness service | `io.github.daniele21.localllm.phonetest.HarnessSharedRuntimeService` |

Harness already authorizes the RedactGuard package identities under the same-publisher signer policy and only for `document-pii-detection`.

## Workstream ledger

| ID | Scope | State | Evidence / outcome |
| --- | --- | --- | --- |
| RG-0 | Bootstrap RedactGuard from `repo-template-sw` Android profile | DONE | independent Android/Compose repo, pinned wrapper/toolchain, CI/build contract |
| HSDK-1 | External Consumer Android SDK boundary | DONE | versioned AAR/POM, external-project compile proof, ABI gate |
| HSDK-2 | Public SDK distribution | DONE | `0.1.0-alpha.1` published to token-free public Maven branch; unauthenticated external consumption proved |
| HHOST-1 | RedactGuard Host authorization | DONE | release/debug RedactGuard identities allowed, same signer, PII use case only |
| RG-2A | PII definitions + strict JSON primitives | DONE | built-in/custom domain and bounded strict JSON |
| RG-2B | Structured protocol + chunk planning | DONE | app-owned limits, schema, deterministic Unicode-safe fragmentation |
| RG-2C | Review/redaction domain | DONE | pending/accept/ignore, source match, overlap block, deterministic placeholders |
| RG-2D | Structured result/finding validation | DONE | no repair, exact source match, deterministic fragment-to-canonical mapping |
| RG-2E | Sequential document analysis | DONE | one-at-a-time chunks, combined final validation, no partial findings, cancellation |
| RG-3A | PDF import/extraction | DONE | SAF source capability + isolated parser + canonical segments |
| RG-3B | PDF redaction export | DONE | validated plan -> newly generated normalized PDF, bounded writer, failed-output cleanup |
| RG-4A | Product UI ownership | DONE | no Harness design-system dependency |
| RG-4B | Definition/custom PII UX domain | DONE | process-local selection/controller |
| RG-4C | Hidden Review projection | DONE | surfaces hidden by default, only explicit current reveal enters presentation state |
| RG-4D | Complete product-flow screens | DONE | import/analyze/review/no-findings/export/success/error/custom PII states |
| RG-5 | Quality corpus/policy migration | DONE | frozen synthetic corpus/policy moved under RedactGuard ownership |
| RG-6A | External SDK consumption | DONE | RedactGuard resolves published SDK without Harness source checkout or credentials |
| RG-6B | Strict Consumer API/Binder adapter | DONE | app-owned port, exact capability/execution identity checks, JSON schema/stateless/no reasoning, cleanup |
| RG-6C | End-to-end app orchestration | DONE | SAF import -> definitions -> sequential analysis -> Review -> redaction plan -> SAF export |
| RG-7A | Repository validation | DONE | PR #29 exact head `98908d0048409f6ac1c1e43b5c7a1620d9faf7ba`: format/compile/tests/lint/assemble all green |
| RG-7B | Physical two-APK gate definition | DONE | signer/package preflight + complete ARM64 runbook committed |
| RG-7C | Physical two-APK execution | **PENDING / BLOCKER** | requires real device + independently built exact APKs |
| HCUT-1 | Remove legacy OMBRA app from Harness | **BLOCKED** | starts only after RG-7C passes |
| HCUT-2 | Remove temporary legacy OMBRA Consumer identity | **BLOCKED** | starts only after RG-7C passes and RedactGuard identity is proven |

## Repository-side completion identity

The complete RedactGuard product flow was merged to `dev` by PR #29 as commit:

`343ab5f4ac26438aac0f1212a66022e1689f9274`

The merge candidate exact head `98908d0048409f6ac1c1e43b5c7a1620d9faf7ba` passed:

```text
Check formatting           PASS
Compile app Kotlin         PASS
Compile JVM unit tests     PASS
Run JVM unit tests         PASS
Run Android Lint           PASS
Assemble debug APK         PASS
```

The CI workflow now keeps these stages separate so future failures identify compile/test/lint/build ownership directly.

## Current dependency graph

All repository implementation dependencies have converged. The only critical path left is evidence and cutover:

```text
Harness Host + public Consumer SDK      RedactGuard dev complete
                \                         /
                 \                       /
                  +---- exact APKs ------+
                           |
                           v
                 RG-7C physical ARM64 E2E
                           |
             +-------------+-------------+
             |                           |
             v                           v
 independent protected-PDF        same-signer Binder,
       verification               failure/recovery proof
             |                           |
             +-------------+-------------+
                           |
                           v
                   physical gate green
                           |
                           v
                    HCUT-1 / HCUT-2
```

## RG-7C — physical ARM64 evidence

Use `docs/evidence/physical-two-apk.md` as the canonical runbook and begin with:

```bash
bash scripts/physical-two-apk-preflight.sh \
  --device <SERIAL> \
  --host-apk <HARNESS_HOST_APK> \
  --app-apk <REDACTGUARD_APK>
```

The run must record exact Harness/RedactGuard commits, APK SHA-256 values, `consumer-android` version, signer certificate SHA-256, device model/Android version and device serial/asset label.

Required scenarios:

1. RedactGuard with Harness absent -> explicit unavailable state and no analysis.
2. Install/start Harness -> recovery to `Harness connesso` without reinstalling RedactGuard.
3. SAF import of a synthetic PDF and successful isolated extraction.
4. At least four built-in PII categories through `document-pii-detection` with no RedactGuard model/runtime selector.
5. Multi-chunk sequential inference with no partial Review result on later-chunk failure.
6. Review hidden by default; explicit reveal then hide; at least one `Oscura` and one `Ignora` decision.
7. Cancellation during active generation with no partial result and session cleanup.
8. Host death/restart during analysis with explicit disconnect/failure, not stale-success behavior.
9. SAF export and independent reopen of the destination PDF: accepted synthetic PII absent, placeholders present, ignored content still present.
10. Failed/unwritable destination -> no success and best-effort partial-output deletion.
11. Process death/relaunch -> no restoration of document text, reveal state or review task.

Emulator evidence, repository CI success, successful SDK resolution or a successful writer return alone cannot satisfy RG-7C.

## Cutover rule

`apps/local-llm-console` in Harness remains a temporary legacy implementation until RG-7C passes. After a green physical evidence package:

1. record the exact evidence identity in both repositories;
2. remove the legacy OMBRA app/module from Harness;
3. remove the temporary legacy OMBRA Consumer package/identity while retaining RedactGuard authorization;
4. retain the generic Consumer fixture in Harness;
5. rerun Harness CI/Consumer SDK validation and one final RedactGuard build against the unchanged published SDK contract.

Until those steps are complete, repository-side migration is complete but cross-repository cutover remains open.
