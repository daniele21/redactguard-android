# repo-template-sw alignment

Status: active
Owner: redactguard-android
Read when: aligning RedactGuard engineering governance, Android lifecycle, product UX/UI and evidence with `daniele21/repo-template-sw`

## Goal

Bring `redactguard-android` to a truthful, machine-checkable `repo-template-sw` 0.5.x baseline with the `android` and `product-ui` profiles adopted, while preserving the existing privacy-first RedactGuard domain/runtime boundaries and improving the product experience in the standard decision order rather than through visual-only redesign.

The target is an L1 production-ready repository baseline with selected L2 controls where they are low-cost and high-value. L2 is not a reason to add speculative infrastructure.

## Non-goals

- Do not add OCR/VLM support; image-only PDFs remain an explicit unsupported/recoverable input until a separate workstream owns OCR.
- Do not move Harness runtime/model responsibilities into RedactGuard.
- Do not redesign Binder, llama.cpp, GGUF/model lifecycle or Host scheduling.
- Do not persist document text, findings, reveal state or review decisions merely to simplify UI state restoration.
- Do not add decorative motion, graphics or a large component framework before task hierarchy and interaction semantics are settled.
- Do not turn every repository-template mechanism into custom RedactGuard machinery when the stock mechanism is sufficient.

## Invariants

- Sensitive document text, findings and user review state remain process-local by default and absent from normal logs/diagnostics.
- There is no silent cloud fallback.
- RedactGuard owns document ingestion, PII policy, analysis orchestration, review/redaction and export; Harness owns local-model runtime, model selection/residency and Binder Host behavior.
- Harness/Binder/native/runtime implementation types stay behind the RedactGuard infrastructure boundary.
- Known failure identity is preserved end-to-end and user-facing failures remain actionable with progressive technical diagnostics.
- Repository/CI/emulator evidence must never be presented as physical-device evidence.
- UX changes follow `user outcome -> task model -> IA/journey -> hierarchy -> disclosure/defaults -> states/recovery -> adaptive/platform -> accessibility -> design system -> motion -> visual polish -> validation`.
- Parallel slices may not write the same canonical owner unless an explicit integration point is named.

## Target profile

```text
repo-template-sw: 0.5.x
maturity target: L1 production-ready
selected L2 controls: repository policy verification, architecture/failure fitness checks, bounded evidence identity, design-system drift checks where valuable
profiles:
  - android
  - product-ui
```

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| RTA-1 | Adopt engineering baseline and repository verifiers | `.engineering/`, `scripts/verify_*`, CI verification hooks | — | yes | READY |
| RTA-2 | Converge agent routing and documentation lifecycle | `AGENTS.md`, `CONTRIBUTING.md`, `docs/README.md`, `docs/current-state.md`, stale completed `docs/workstreams/` | — | yes | READY |
| RTA-3 | Add build identity, artifact lineage and bounded local artifact lifecycle | build identity/config, build/package scripts, artifact manifest/checksum/retention logic | — | yes | READY |
| RTA-4 | Define RedactGuard product-experience contract | `design/ux-contract.json`, `design/README.md`, bounded `design/reference/` contract | — | yes | READY |
| RTA-5 | Define brand/design-system source of truth and Compose theme primitives | `design/brand-kit.json`, theme/tokens/canonical UI primitives | RTA-4 | yes | BLOCKED |
| RTA-6 | Reshape the critical product journey and information/action hierarchy | product flow state/surfaces/navigation/microcopy; no visual-only polish | RTA-4 | yes | BLOCKED |
| RTA-7 | Implement adaptive Android behavior and accessibility contract | adaptive layout owners, semantics, text scaling/targets/status announcements, accessibility tests | RTA-4 | yes | BLOCKED |
| RTA-8 | Apply design system and purposeful motion/visual polish | screen composition using RTA-5 primitives; motion tokens/transitions | RTA-5,RTA-6,RTA-7 | no | BLOCKED |
| RTA-9 | Add product-experience regression evidence | `androidTest`/Compose UI tests, critical state tests, adaptive/accessibility evidence, selective visual regression | RTA-6,RTA-7,RTA-8 | yes | BLOCKED |
| RTA-10 | Turn `smoke` and `e2e` into real operating-contract commands | `.engineering/commands.json`, device/app cleanup helpers, critical packaged-app journey wiring | RTA-1,RTA-3,RTA-4 | yes | BLOCKED |
| RTA-11 | Align repository governance and canonical-branch policy | repository settings/runbook evidence for `dev`/`main`, required CI and merge policy | RTA-1 | yes | BLOCKED |
| RTA-12 | Final integration, exact-head validation and workstream finalization | integration only; durable docs/evidence handoff; delete this plan when complete | RTA-2,RTA-3,RTA-5,RTA-6,RTA-7,RTA-8,RTA-9,RTA-10,RTA-11 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

## Parallel execution model

### Wave A — execute immediately in parallel

```text
Lane A — engineering substrate       RTA-1
Lane B — docs/governance cleanup     RTA-2
Lane C — build/artifact lifecycle    RTA-3
Lane D — UX contract                 RTA-4
```

These lanes have intentionally separate canonical write ownership.

### Wave B — after the UX contract exists

```text
                 RTA-4
               /   |   \
              v    v    v
          RTA-5  RTA-6  RTA-7
           theme  flow  adaptive/a11y
```

RTA-5, RTA-6 and RTA-7 may execute in parallel, but their integration rules are:

- RTA-5 owns semantic tokens/theme/primitives, not product-flow semantics;
- RTA-6 owns task flow, hierarchy and microcopy, not raw visual token values;
- RTA-7 owns adaptive/accessibility behavior and evidence, not brand treatment.

### Wave C — product integration and evidence

```text
RTA-5 + RTA-6 + RTA-7 -> RTA-8
RTA-6 + RTA-7 + RTA-8 -> RTA-9
RTA-1 + RTA-3 + RTA-4 -> RTA-10
RTA-1 -> RTA-11
```

`RTA-9`, `RTA-10` and `RTA-11` can run in parallel once their prerequisites are satisfied.

### Wave D — convergence

`RTA-12` starts only after every repository-owned implementation/evidence slice is green. Physical two-APK evidence that genuinely requires the real device remains an explicit external gate and must be reported as pending rather than inferred from CI.

## Current executable slices

`RTA-1`, `RTA-2`, `RTA-3`, `RTA-4`

### RTA-1 — Engineering baseline and verifier adoption

Acceptance:

- `.engineering/baseline.json` declares the exact `repo-template-sw` source/version, target maturity and `android` + `product-ui` profiles;
- `.engineering/documentation-policy.json` exists and reflects RedactGuard's bounded-doc policy;
- project-local copies/adaptations of the standard repository, operations, docs, agent-context and product-experience verification scripts are present when applicable;
- `commands.json` remains the canonical native command router rather than gaining a second wrapper layer;
- CI executes deterministic standard checks before claiming alignment;
- stock template rules are customized only where RedactGuard has a concrete project-specific reason.

Validation:

```text
python3 scripts/verify_repository.py
python3 scripts/verify_operations.py
python3 scripts/verify_docs.py
python3 scripts/verify_agent_context.py
python3 scripts/verify_product_experience.py
```

### RTA-2 — Agent routing and documentation lifecycle

Acceptance:

- root `AGENTS.md` becomes a bounded routing layer rather than a short invariant list with missing standard navigation;
- `CONTRIBUTING.md` describes the canonical branch/workstream/validation flow without duplicating generic standard text unnecessarily;
- `docs/current-state.md` reflects the actual current `dev` state and does not describe already-integrated repository work as still unimplemented;
- completed workstreams are finalized: durable behavior moves to architecture/features/tests and completed plans are deleted by default;
- only genuinely active workstreams remain under `docs/workstreams/`;
- the current-state document links this alignment workstream exactly once while it is active.

Validation:

```text
python3 scripts/verify_docs.py
python3 scripts/verify_agent_context.py
```

### RTA-3 — Build identity and artifact lifecycle

Acceptance:

- each material debug/internal/release build can carry a unique build ID distinct from product version;
- build identity includes source revision and distinguishes dirty local source when applicable;
- promoted distributable artifacts have machine-readable identity plus SHA-256 checksum;
- a failed/partial build cannot be mistaken for a successful promoted artifact;
- local successful comparable artifacts have bounded retention, defaulting to the latest two unless RedactGuard documents a better reason;
- a material comparable build produces a useful build delta rather than relying on Git log alone;
- existing Play signing remains fail-closed and no signing secret is committed.

Validation:

```text
build two comparable artifacts -> identities differ
artifact manifest -> product version + build ID + source revision
checksum verifies
failed staging build -> no promoted valid-looking artifact
retention -> bounded
```

### RTA-4 — Product-experience contract

Acceptance:

`design/ux-contract.json` explicitly owns at least:

- primary user and job-to-be-done;
- successful outcome;
- critical journey: `input -> choose protection -> local analysis -> review -> export -> verification/recovery`;
- primary/secondary/destructive action hierarchy per major surface;
- progressive-disclosure levels;
- which Harness/runtime concepts are user-visible versus diagnostic-only;
- sensible defaults;
- loading/empty/success/warning/error/disabled/offline-or-runtime-unavailable/partial-result applicability;
- recovery expectations;
- supported Android window classes/orientations/input assumptions;
- accessibility target;
- critical journeys that require E2E evidence;
- design source of truth and bounded reference-view policy.

Product terminology must model the user's task. For example, normal surfaces should prefer concepts such as `AI locale pronta/non disponibile` over infrastructure naming such as `Harness connesso`, while technical identity remains available in diagnostics where useful.

Validation:

```text
python3 scripts/verify_product_experience.py
manual contract review against PRODUCT-EXPERIENCE-CONTRACT.md
```

## Blocked slice acceptance

### RTA-5 — Brand/design-system and Compose primitives

Acceptance:

- `design/brand-kit.json` is the canonical visual-language owner;
- `RedactGuardTheme` owns color, typography, shape and motion semantics instead of plain unconfigured `MaterialTheme`;
- raw repeated spacing/color/radius values are replaced by a small intentional semantic system where reuse justifies it;
- canonical components exist only for repeated semantic roles (for example product status, primary action, review item, progress, error/recovery), avoiding a speculative component library;
- light/dark behavior is explicitly decided;
- critical meaning never depends on color alone.

### RTA-6 — Critical journey / structural UX

Acceptance:

- import, definition selection, analysis, review, no-findings, export and error states follow the UX contract;
- each surface has one obvious primary decision/action;
- infrastructure terminology does not dominate normal product interaction;
- analysis exposes truthful useful progress/status rather than decorative indefinite waiting when phase information exists;
- review minimizes cognitive load while preserving explicit reveal, redact/ignore decisions and fail-closed export eligibility;
- back/cancel/new-document/retry behavior is explicit and recoverable;
- custom PII and technical diagnostics remain progressively disclosed.

### RTA-7 — Adaptive Android and accessibility

Acceptance:

- compact phone portrait is not the only implicit layout contract;
- important compact/landscape/expanded behavior is explicitly implemented/tested where relevant;
- large screens use additional space to preserve context rather than simply stretching a single column;
- touch targets, semantic labels, focus/order, text scaling and status/error announcements are addressed with Android-native semantics;
- non-essential motion respects reduced-motion expectations where the platform surface supports it;
- process-local security behavior survives configuration/layout changes without accidentally persisting sensitive task state.

### RTA-8 — Motion and visual polish

Acceptance:

- motion is introduced only for feedback, continuity, state transition, progress, hierarchy or meaningful completion;
- frequent actions remain restrained and fast;
- transitions do not delay critical interaction or conceal state changes;
- visual hierarchy is driven first by typography/spacing/grouping, not excessive cards/borders/elevation;
- graphics are optional and functional/brand-supportive, never required to understand the workflow;
- all screens consume canonical semantic design primitives.

### RTA-9 — UX regression evidence

Acceptance:

- critical Compose journeys have native UI/instrumentation evidence where JVM projection tests are insufficient;
- important loading/empty/error/disabled/recovery states have regression coverage;
- accessibility has automated evidence plus a documented manual device/TalkBack check for the critical path;
- representative adaptive contexts are exercised;
- selective screenshot/visual regression protects stable high-risk surfaces without pixel-freezing the whole app;
- failed UI/E2E evidence is bounded and identity-bearing.

### RTA-10 — Operating-contract smoke and E2E

Acceptance:

- `.engineering/commands.json` no longer leaves `smoke` and `e2e` as vague planned placeholders;
- `smoke` proves built-APK install/launch/minimal viability and has deterministic cleanup;
- `e2e` owns a small high-value complete product journey and representative recovery path rather than broad UI scripting;
- when the claim depends on Harness + RedactGuard assembly, the two-APK physical runbook remains the stronger evidence boundary;
- device/app/test-fixture/temp evidence is cleaned on success, failure and interrupt where project-owned.

### RTA-11 — Canonical branch governance

Acceptance:

- `dev` and `main` have an explicit canonical role;
- required validation checks and merge policy are configured for canonical branches where GitHub repository settings permit it;
- direct bypass is either prevented or explicitly documented as an owner-only exceptional path;
- branch policy does not claim stronger protection than is actually configured.

### RTA-12 — Final convergence

Acceptance:

- exact integration head passes the full repository command/verification set;
- product UI evidence matches the final code, not a prior design branch;
- architecture/current-state/design contracts match current behavior;
- no completed alignment plans remain active;
- all generated evidence/artifacts follow bounded retention rules;
- physical-device gaps are explicitly `PENDING` unless actually run;
- after durable knowledge is transferred, this workstream is deleted by default.

## Integration points

- **Engineering -> CI:** RTA-1 defines machine-checkable policy; later lanes add checks through this canonical mechanism rather than independent ad-hoc workflows.
- **UX contract -> UI:** RTA-4 is the semantic owner. RTA-5/6/7 may refine implementation but may not silently change the critical journey or hierarchy.
- **Design system -> product screens:** RTA-5 owns visual/component semantics; RTA-8 integrates those primitives into screens.
- **Product flow -> E2E:** RTA-6 names the final journeys/states; RTA-9/RTA-10 turn only high-value claims into UI/E2E evidence.
- **Build identity -> device evidence:** RTA-3 identity must be used by RTA-9/RTA-10 and the existing physical two-APK runbook so evidence is traceable to exact artifacts.
- **Existing failure contract:** no UX slice may collapse the stable product failure identities already implemented.

## Proposed implementation branches

The lanes should be independently branchable from the same current `dev` base and integrated in dependency order:

```text
agent/rta-engineering-baseline       -> RTA-1
agent/rta-doc-lifecycle              -> RTA-2
agent/rta-build-lifecycle            -> RTA-3
agent/rta-ux-contract                -> RTA-4

agent/rta-design-system              -> RTA-5 (after RTA-4)
agent/rta-product-flow-ux            -> RTA-6 (after RTA-4)
agent/rta-adaptive-accessibility     -> RTA-7 (after RTA-4)

agent/rta-ui-integration             -> RTA-8
agent/rta-ui-evidence                -> RTA-9
agent/rta-operating-e2e              -> RTA-10
agent/rta-repo-governance            -> RTA-11
agent/rta-final-convergence          -> RTA-12
```

Do not stack independent Wave A branches on one another. Rebase/retarget each dependent lane only after its declared dependency integrates to `dev`.

## Validation strategy

Use the cheapest deterministic evidence while developing, then expand by blast radius.

Repository gate target:

```text
repo-template repository verification
repo-template operations verification
repo-template docs/agent-context verification
repo-template product-experience verification
Spotless
failure-contract guard
Kotlin compilation
JVM tests
Android Lint
debug APK
minified release APK
instrumented/Compose UI tests when introduced
smoke built APK
bounded critical E2E
physical two-APK evidence only when a real device is actually used
```

No single screenshot, JVM unit suite or CI build is sufficient to claim full product-experience readiness.

## Durable documentation destinations

- `.engineering/baseline.json`: adopted standard/profile/maturity/skill identity.
- `.engineering/commands.json`: canonical native operating intents.
- `.engineering/documentation-policy.json`: bounded documentation/context policy.
- `AGENTS.md`: repository routing and durable invariants only.
- `docs/architecture.md`: stable architecture/resource/security ownership changes only.
- `docs/features/`: durable current product behavior where extra detail is justified.
- `docs/current-state.md`: one short repository-level executable-state ledger.
- `design/ux-contract.json`: canonical product-experience semantics.
- `design/brand-kit.json`: canonical visual/motion/voice design semantics.
- `design/reference/`: only bounded key reference views, never a screenshot archive.
- tests/scripts/CI: executable truth for policy, lifecycle, failure and product-experience claims.

## Completion

This workstream is complete only when repository governance, build/artifact lifecycle, product UX semantics, UI implementation, adaptive/accessibility behavior, validation/evidence and durable documentation agree on the same exact integration state.

On completion:

1. transfer durable behavior/decisions to their canonical owners;
2. update `docs/current-state.md` with the resulting maturity/evidence state;
3. remove completed legacy workstream documents whose durable knowledge has already moved;
4. delete this workstream by default;
5. keep any unexecuted physical-device requirement explicitly pending in the appropriate evidence/runbook owner.
