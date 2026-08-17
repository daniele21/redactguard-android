# OMBRA to RedactGuard cross-repository migration

Status: active
Document type: coordinated implementation plan
Owner: redactguard-android + android-local-llm-harness
Canonical scope: redactguard.cross-repo-migration
Started: 2026-08-17

## Goal

Move the OMBRA Android product out of `daniele21/android-local-llm-harness` and establish it as the independent `daniele21/redactguard-android` application without weakening the existing local-only, fail-closed and two-APK security model.

The end state must prove that RedactGuard is developed, built, versioned and tested from a repository independent from Harness and consumes only a versioned public Android Consumer SDK. Harness continues to own the local model/runtime and Binder host; RedactGuard owns the complete document-redaction product workflow.

The migration is complete only after a physical cross-repository two-APK workflow succeeds using independently built artifacts from the two repositories and the legacy OMBRA application module is removed from Harness.

## Non-negotiable boundaries

RedactGuard owns:

- Android product UI and navigation;
- PDF import, extraction and segmentation;
- PII built-in/custom definitions;
- prompt/schema composition for `document-pii-detection`;
- consumer-side structured-result validation;
- sequential document analysis orchestration;
- human review, reveal, accept/ignore decisions and overlap/conflict policy;
- deterministic redaction and new-PDF export;
- synthetic PII quality corpus and quality-policy execution;
- RedactGuard-specific privacy/security review and product evidence.

Harness owns:

- public Consumer API semantics and compatibility/versioning;
- Android Binder protocol, client implementation and host service;
- caller authorization, package/signing policy and use-case allowlist;
- host-owned `document-pii-detection` binding and inference preset;
- model catalog, installation, integrity, selection and residency;
- runtime admission, scheduling, cancellation, memory/resource management and llama.cpp;
- host diagnostics and shared-runtime evidence.

RedactGuard must not depend directly on Harness source modules such as runtime-core, model-store, model-profile, llama.cpp, observability internals or project-source Binder modules. The final app must not package GGUF/GGML artifacts.

The Harness UI design system is not part of the public platform contract. RedactGuard must own the components/theme that are required by the product instead of taking a remote dependency on `ui:design-system`.

## Target topology

```text
Repository A: android-local-llm-harness

  Local AI Host APK
      |
      +-- model/runtime ownership
      +-- document-pii-detection policy
      +-- Binder host service
      +-- published Consumer Android SDK
                        |
                        | versioned Maven/AAR dependency
                        v
Repository B: redactguard-android

  RedactGuard APK
      |
      +-- PDF import/extraction
      +-- PII definitions
      +-- analysis request/validation
      +-- Review
      +-- redaction/export
                        |
                        | Binder IPC at runtime
                        v
                 Local AI Host APK
```

## Engineering baseline

RedactGuard adopts `daniele21/repo-template-sw` as a bootstrap/audit source, not as a runtime dependency.

Applicable baseline:

- base engineering standard;
- Android profile;
- stack-native Gradle/Android tooling mapped into `.engineering/commands.json`;
- current-state ledger and bounded active workstreams;
- architecture/security ownership documentation;
- CI for check/test/build gates;
- explicit build/source identity and bounded artifacts;
- small, high-value E2E set on the real APK/device surface.

The `local-ai` profile is intentionally **not** adopted by RedactGuard because this repository must not load or orchestrate model residency. If RedactGuard begins to own GGUF loading, LLM memory residency, decode scheduling or backend lifecycle, the architecture boundary has regressed and must be reviewed rather than normalized.

## Identity target

Target product identity:

```text
Repository:      daniele21/redactguard-android
ApplicationId:   redactguard
UseCaseId:       document-pii-detection
Release package: io.github.daniele21.redactguard
Debug package:   io.github.daniele21.redactguard.debug
```

The host remains the owner of the model and preset selected for `document-pii-detection`.

Package and logical identity changes must land with matching Harness authorization changes so there is never an undocumented permissive compatibility fallback.

## Migration strategy

The migration uses a strangler approach rather than a big-bang move:

1. preserve the current integrated OMBRA implementation as the known-good behavioral reference;
2. publish a real externally consumable Harness Consumer SDK;
3. bootstrap RedactGuard independently;
4. migrate pure product capabilities in parallel while the SDK work proceeds;
5. connect RedactGuard only through the published SDK;
6. run cross-repository contract, emulator and physical-device evidence;
7. remove the legacy OMBRA module from Harness only after the external application is green.

No source-copy of Binder client/contracts is accepted as the final state. Temporary source mirroring is also avoided because it creates two contract owners and invalidates the purpose of the extraction.

---

# Workstreams

## RG-0 — Repository bootstrap and engineering contract

Owner: RedactGuard
State: IN PROGRESS
Dependencies: none
Parallel with: HSDK-1, HHOST-1, RG-1, RG-2, RG-3, RG-4

Deliverables:

- `main` and `dev` branches established;
- root `AGENTS.md`;
- `.engineering/commands.json` mapping `setup`, `doctor`, `dev`, `check`, `test`, `e2e`, `build`, `smoke`, `package`, `stop`, `clean` to native Android/Gradle commands;
- Android Gradle wrapper/toolchain and pinned dependency policy;
- README, architecture, current-state, security and contribution docs;
- CI skeleton for formatting/static checks, unit tests, Android Lint and build;
- explicit build identity/retention approach appropriate to Android artifacts.

Exit criteria:

- a clean checkout can diagnose its Android toolchain and build the application shell using documented commands;
- CI is green on the bootstrap branch;
- no Harness source checkout is required merely to configure the RedactGuard build.

## HSDK-1 — Publishable Harness Consumer Android SDK

Owner: Harness
State: TODO
Dependencies: none
Parallel with: RG-0, RG-1, RG-2, RG-3, RG-4, HHOST-1
Critical path: YES

Goal: turn the already-implemented Binder client/contracts boundary into an artifact consumable by an external repository.

Deliverables in `android-local-llm-harness`:

- define the supported public artifact surface and coordinates;
- package Consumer contracts + Binder client dependencies without exposing project-source dependencies to consumers;
- version SDK independently from the RedactGuard app;
- publish a reproducible Maven/AAR artifact for local/CI integration first, then the selected durable package channel;
- consumer ProGuard/R8 rules included;
- POM/module metadata resolves all required transitive public pieces;
- public API/ABI compatibility gate;
- SDK artifact manifest/SHA-256/source revision/build identity;
- minimal external-consumer sample/fixture consumes the artifact rather than project source;
- documentation for install, host discovery, capabilities, sessions, streaming/cancellation and typed failures.

Design constraint:

RedactGuard should ideally require one public Android dependency. Internal Harness modules may remain separate but their topology must not become an integration burden for consumers.

Exit criteria:

- a fresh external Gradle project can depend on the produced artifact without composite build, source include, submodule or Harness checkout;
- packaged consumer fixture passes bind/prepare/session/generate/cancel using the artifact;
- no runtime/model implementation leaks into the SDK artifact.

## HHOST-1 — RedactGuard host identity and authorization

Owner: Harness
State: TODO
Dependencies: RG identity decision only; identity is fixed by this plan
Parallel with: HSDK-1, RG-0..RG-4

Deliverables:

- add release/debug RedactGuard package identities to exact host policy;
- map logical `ApplicationId("redactguard")` to `document-pii-detection` only;
- preserve signature-level permission enforcement;
- preserve fail-closed UID/package resolution and signing-certificate verification;
- remove legacy Console/OMBRA package authorization only in the final cleanup wave;
- unit tests proving debug/release allowlist behavior and rejecting unknown package/application/use-case combinations.

Exit criteria:

- same-signer RedactGuard package is authorized only for intended use cases;
- independently signed package is denied before runtime negotiation;
- old identity remains only as an explicitly temporary migration compatibility entry until CUT-1.

## RG-1 — Android application shell and product identity

Owner: RedactGuard
State: TODO
Dependencies: RG-0 minimum Gradle bootstrap
Parallel with: HSDK-1, HHOST-1, RG-2, RG-3, RG-4

Deliverables:

- Android application module with `io.github.daniele21.redactguard` release identity and `.debug` suffix;
- Compose app entrypoint;
- RedactGuard-owned theme/components required by migrated screens;
- no model/runtime/Harness implementation modules;
- build/version/source identity visible in a developer/about surface or build manifest as appropriate;
- signing configuration externalized with no secrets in source control.

Exit criteria:

- app launches independently without Harness installed and shows the correct typed/unavailable state;
- package inspection proves no GGUF/GGML/native llama.cpp payload.

## RG-2 — Pure product domain migration

Owner: RedactGuard
State: TODO
Dependencies: none for extraction into pure Kotlin modules; RG-0 before final integration
Parallel with: HSDK-1, RG-1, RG-3, RG-4, HHOST-1

Migrate and preserve behavior for:

- built-in PII definitions and custom-definition validation;
- normalized source/segment identities;
- chunk planning rules and public-limit budgeting;
- strict JSON parsing;
- finding validation and exact source-surface mapping;
- duplicate/overlap/conflict policy;
- review decision state;
- deterministic replacement ordering and placeholders;
- sensitive process-local task lifecycle.

Rules:

- change package/product naming while preserving behavioral invariants;
- migrate tests with the owning code;
- do not preserve imports whose only purpose was monorepo convenience;
- keep document/model content out of normal logs.

Exit criteria:

- pure domain tests pass in RedactGuard with no Harness runtime/model implementation dependency;
- behavior matches the known-good OMBRA baseline for synthetic fixtures.

## RG-3 — PDF import, extraction and export migration

Owner: RedactGuard
State: TODO
Dependencies: none for implementation extraction; RG-1 for Android wiring
Parallel with: HSDK-1, RG-2, RG-4

Deliverables:

- Storage Access Framework PDF import;
- isolated parser boundary and bounded extraction/segmentation;
- typed unsupported/encrypted/malformed/image-only outcomes;
- deterministic resource cleanup on success/failure/cancel;
- new-PDF export with accepted redactions only;
- independent output verification test path;
- synthetic PDF fixture generator and license/provenance documentation.

Exit criteria:

- import -> extract -> deterministic export fixture tests pass without any LLM;
- accepted source surfaces are absent from generated output; ignored surfaces remain;
- source attachments/hidden source text are not silently copied into output.

## RG-4 — Product UI, Review and accessibility migration

Owner: RedactGuard
State: TODO
Dependencies: RG-1 app shell; can start component extraction immediately
Parallel with: HSDK-1, RG-2, RG-3

Deliverables:

- Import screen;
- definition selection/custom definition UI;
- local-analysis progress/cancel states;
- safe Review with values hidden by default;
- explicit reveal for one occurrence;
- `Oscura` / `Ignora` decisions;
- conflict/pending blockers;
- export progress/success/failure/retry;
- inaccessible/hidden sensitive text absent from semantics;
- portrait/landscape/large-font coverage;
- RedactGuard-owned theme/design components.

Exit criteria:

- all product states render from fake application ports without Harness source dependencies;
- Compose semantics/screenshot tests cover critical states.

## RG-5 — Quality corpus and policy migration

Owner: RedactGuard
State: TODO
Dependencies: none for corpus migration; RG-6 for real execution
Parallel with: HSDK-1, RG-1..RG-4

Migrate the active synthetic corpus and policy while preserving exact identity/history:

- current 32-case v2 corpus;
- positive examples for all built-in categories;
- near-miss, Unicode, prompt-injection, repeated/overlap and no-PII cases;
- pre-registered aggregate/per-category thresholds;
- corpus identity/hash and policy identity/hash;
- deterministic evaluator independent from UI.

Exit criteria:

- corpus and policy identity are reproducible in RedactGuard;
- migration does not change thresholds based on observed post-migration performance;
- any intentional corpus change creates a new corpus identity/version.

## RG-6 — Harness SDK adapter and application composition

Owner: RedactGuard
State: BLOCKED on HSDK-1
Dependencies: HSDK-1, RG-1, RG-2
Parallel with: late RG-3/RG-4/RG-5 completion
Critical path: YES

Deliverables:

- depend only on the published Harness Consumer Android SDK;
- configure the exact host package/service per debug/release channel;
- connect automatically and map typed connection states;
- resolve only `document-pii-detection`;
- accept only host-owned supported/default preset behavior;
- require JSON-schema/stateless/reasoning-disabled capabilities;
- create/close consumer sessions per operation;
- sequential chunk generation;
- execution identity validation;
- reject reasoning deltas and malformed/invalid application JSON;
- cancellation/disconnect/host-death behavior without silent replay.

Exit criteria:

- no Gradle `project(...)`, composite build or source path points into Harness;
- fake SDK/port tests and real packaged-SDK integration tests are green.

## INT-1 — Cross-repository build and contract gate

Owner: joint
State: BLOCKED
Dependencies: HSDK-1, HHOST-1, RG-6
Parallel with: RG-5 quality execution preparation
Critical path: YES

Build provenance must record both repositories independently:

```text
Harness host artifact
  - Harness source revision
  - host build ID/version
  - Consumer SDK version/hash

RedactGuard artifact
  - RedactGuard source revision
  - app build ID/version
  - resolved Consumer SDK version/hash
```

Gate:

- build Harness Host APK from Harness checkout;
- publish/resolve exact Consumer SDK artifact;
- build RedactGuard APK from RedactGuard checkout;
- verify expected package/signing identities;
- install both artifacts;
- bind + capability discovery;
- execute one deterministic synthetic document inference;
- assert structured result reaches RedactGuard;
- verify no source-level cross-repository dependency exists.

Exit criteria:

- a RedactGuard build can be reproduced from its own repository plus declared package repositories only;
- cross-repo contract mismatch fails explicitly rather than silently substituting behavior.

## E2E-1 — Cross-repository debug physical smoke

Owner: joint
State: BLOCKED
Dependencies: INT-1, representative supported Qwen3.5 installed in host
Parallel with: RG-5 quality execution
Critical path: YES

Physical-device happy path:

1. build/install Harness debug Host APK from Harness repo;
2. install/load a reviewed Qwen3.5 artifact;
3. build/install RedactGuard debug APK from RedactGuard repo;
4. observe `Harness connesso` equivalent;
5. import synthetic text PDF;
6. select built-in PII definitions;
7. run `document-pii-detection` through Binder;
8. reach Review with validated findings;
9. exercise reveal and `Oscura`/`Ignora`;
10. export a new PDF;
11. independently assert accepted source values absent and ignored values present.

This is a physical integration smoke, not yet release evidence.

## E2E-2 — Full physical release evidence

Owner: joint
State: BLOCKED
Dependencies: E2E-1, RG-5 quality gate, release signing/versioning/security review
Parallel with: final docs/release review

Must cover at minimum:

- host absent -> install/recovery;
- same-publisher authorization;
- independent-signer denial;
- import + built-in/custom definition analysis;
- multi-chunk sequential inference;
- cancellation during extraction and inference;
- host death/disconnect and explicit recovery;
- hidden/revealed review semantics;
- successful export + independent verification;
- destination/write failure cleanup;
- process recreation clears sensitive task state;
- privacy-safe client wall time versus host metrics;
- exact build/model/SDK/corpus/policy identities.

Exit criteria:

- evidence is captured on representative physical Android hardware;
- no real PII, prompt/output content, signing material or private model path is retained in normal evidence;
- both repositories point to the exact cross-repo evidence identity.

## CUT-1 — Remove OMBRA from Harness

Owner: Harness
State: BLOCKED
Dependencies: E2E-1 mandatory; E2E-2 preferred before release-claim cleanup
Parallel with: RedactGuard release hardening after the dependency is satisfied
Critical path: final cleanup

Remove from Harness:

- `apps/local-llm-console` OMBRA product code;
- PDF dependencies and document-specific UI/domain code;
- OMBRA/RedactGuard design-system components that no longer serve Harness;
- obsolete Console/OMBRA package identities and migration aliases;
- duplicated PII quality corpus/policy once RedactGuard is canonical;
- obsolete docs that describe RedactGuard as an in-repo app.

Keep in Harness:

- Consumer API contracts and Android SDK;
- Binder host/client implementation;
- generic packaged consumer fixture;
- host-owned `document-pii-detection` policy/binding;
- cross-app security/compatibility tests;
- shared-runtime evidence tooling.

Exit criteria:

- Harness builds/tests without RedactGuard product source;
- RedactGuard builds/tests without Harness source checkout;
- generic consumer fixture still proves the platform independently from RedactGuard.

---

# Parallel execution waves

## Wave A — start immediately

These tasks have no blocking dependency on one another:

```text
RG-0  RedactGuard repository/bootstrap
HSDK-1 Harness publishable Consumer SDK
HHOST-1 Harness RedactGuard identity/allowlist
RG-2  product-domain extraction/migration
RG-3  PDF pipeline extraction/migration
RG-4  UI/Review extraction and RedactGuard design ownership
RG-5  quality corpus/policy migration
```

The migration should use separate branches/PRs per ownership slice where practical so domain/PDF/UI/evaluation work does not wait for the SDK.

## Wave B — converge product pieces

```text
RG-1 app shell/identity
RG-2 domain
RG-3 PDF
RG-4 UI
RG-5 evaluation
      \ | /
       RedactGuard builds without real Harness connection
```

A fake application port is used until the published SDK is available.

## Wave C — critical integration path

```text
HSDK-1 ----+
HHOST-1 ---+--> RG-6 --> INT-1 --> E2E-1
RG-1/2 ---+
```

This is the path to optimize. Work outside this path must not unnecessarily block it.

## Wave D — evidence and cleanup

```text
RG-5 quality execution --------+
E2E-1 -------------------------+--> E2E-2 --> CUT-1
privacy/security/release ------+
```

## Dependency graph

```text
                 +--> RG-2 --+
RG-0 --> RG-1 ---+--> RG-3   |
                 +--> RG-4   +------+
                 +--> RG-5   |      |
                                    v
HSDK-1 ---------------------------> RG-6 --> INT-1 --> E2E-1 --> E2E-2 --> CUT-1
HHOST-1 ----------------------------^                         ^
RG-5 quality execution --------------------------------------+
```

The intentionally broad parallel fan-out is before `RG-6`. The intentionally narrow serialized path begins only when a real published SDK must be consumed.

---

# Branch and PR strategy

RedactGuard:

- canonical integration branch: `dev`;
- feature branches: `agent/<task-id>-<slug>`;
- merge small independent slices to `dev` when their own gates are green;
- `main` receives reviewed release/integration milestones, not partially migrated product state.

Harness:

- continue from its canonical `dev` branch;
- SDK, host identity and final cleanup should use separate feature branches/PRs because they have different rollback surfaces;
- do not remove the existing OMBRA path in the same PR that first publishes the SDK.

Cross-repository PR descriptions must record related task/PR identifiers in the other repository when a compatibility pair is required.

# Source-of-truth transition

During migration, ownership changes deliberately:

| Capability | Before migration | During migration | After cross-repo cutover |
| --- | --- | --- | --- |
| Consumer SDK | Harness source modules | Harness source + published artifact | Harness published artifact |
| PII product domain | Harness OMBRA module | Harness baseline + RedactGuard candidate | RedactGuard |
| PDF pipeline | Harness OMBRA module | Harness baseline + RedactGuard candidate | RedactGuard |
| Review/export UI | Harness OMBRA module | Harness baseline + RedactGuard candidate | RedactGuard |
| Quality corpus/policy | Harness docs/tests | mirrored only as migration evidence | RedactGuard |
| Host PII use case | Harness | Harness | Harness |
| Runtime/model | Harness | Harness | Harness |

No behavioral divergence should be maintained long-term. Once a RedactGuard slice becomes canonical, new product behavior belongs there.

# Validation policy during migration

Every slice uses the cheapest test capable of proving its invariant:

- pure domain: JVM unit tests;
- PDF parser/export: Android/JVM fixture tests as appropriate;
- UI/review: Compose semantics/screenshot tests;
- Consumer adapter: contract/fake SDK tests;
- SDK packaging: external Gradle consumer fixture;
- cross-app boundary: installed APK/Binder integration;
- final product claim: physical-device two-APK E2E;
- model quality: exact versioned synthetic corpus against exact reviewed model/config.

Emulator evidence is preflight only and must not be represented as physical production/release evidence.

# Migration safety / rollback

Until E2E-1 passes, the current in-Harness OMBRA implementation remains available as the behavioral rollback/reference path.

Do not delete or rename the old host authorization identity before a RedactGuard artifact can bind successfully under the new identity.

If SDK publication or external consumption exposes a contract defect, fix the public contract in Harness and issue a new SDK version; do not work around the defect by adding RedactGuard-only copies of transport/domain types.

If migrated RedactGuard behavior differs from the known-good OMBRA baseline, classify the difference explicitly as:

- intended product change;
- migration defect;
- previously latent defect discovered by extraction.

Only intended changes should modify expected fixtures/policy.

# Definition of done

The cross-repository extraction is DONE only when all are true:

1. RedactGuard repository satisfies the adopted base + Android engineering baseline.
2. RedactGuard source contains the complete product flow from PDF import through new-PDF export.
3. Harness publishes a versioned Consumer Android SDK usable without a Harness source checkout.
4. RedactGuard depends on that SDK artifact and no Harness project/composite/source modules.
5. RedactGuard owns its design system/product UI; Harness UI is not a consumer dependency.
6. Harness authorizes exact RedactGuard identities using the intended signature/package/application/use-case policy.
7. The active synthetic quality corpus/policy are canonical and reproducible in RedactGuard.
8. Cross-repository debug physical smoke proves real Binder inference and PDF output.
9. Required release-like physical two-APK evidence, signer denial, cancellation/recovery and privacy gates pass for the candidate being claimed.
10. The old OMBRA/Console product module and obsolete product-specific docs/dependencies are removed from Harness.
11. Harness generic consumer fixture still proves the platform without RedactGuard.
12. Each repository can be cloned, built and validated independently using its documented operating commands.

# Immediate next actions

Start these branches/work items in parallel:

- `RG-0`: finish RedactGuard base/Android bootstrap and CI;
- `HSDK-1`: design/package the public Consumer Android SDK in Harness;
- `HHOST-1`: introduce RedactGuard package/application identity alongside temporary legacy OMBRA identity;
- `RG-2`: migrate pure PII/analysis/review domain + tests;
- `RG-3`: migrate PDF import/parser/export + fixture tests;
- `RG-4`: migrate product UI while replacing Harness design-system ownership;
- `RG-5`: migrate corpus v2 + quality policy without changing identities/thresholds.

Do **not** start `CUT-1` until the external RedactGuard application passes the real cross-repository integration gate.
