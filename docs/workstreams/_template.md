# <WORKSTREAM_TITLE>

Status: active
Owner: <DOMAIN_OR_REPOSITORY>
Read when: implementing or coordinating <WORKSTREAM_SCOPE>

## Product intent — only for PRODUCT_FEATURE / PRODUCT_STRATEGIC

<!-- Remove this section for PRODUCT_NONE / PRODUCT_LOCAL. Shape material product work first with skills/shape-product-change/SKILL.md. -->

- Product depth: <PRODUCT_FEATURE_OR_PRODUCT_STRATEGIC>
- User / problem / desired outcome: <COMPACT_PRODUCT_INTENT>
- Material risks / assumptions: <VALUE_USABILITY_FEASIBILITY_VIABILITY_ONLY_WHERE_MATERIAL>
- Success: <ACCEPTANCE / OUTCOME / PRODUCT_IMPACT_WHEN_MATERIAL>
- Post-release question: <QUESTION_OR_NA>

## Goal

<ONE_CLEAR_OUTCOME>

## Non-goals

- <EXPLICITLY_EXCLUDED_SCOPE>

## Invariants

- <INVARIANT_THAT_MUST_REMAIN_TRUE>

## Work graph

| ID | Work | Owns/writes | Depends on | Parallel | State |
| --- | --- | --- | --- | --- | --- |
| WS-1 | <bounded slice> | <paths/boundary> | — | yes | READY |
| WS-2 | <bounded slice> | <paths/boundary> | WS-1 | no | BLOCKED |

Allowed states: `READY`, `ACTIVE`, `BLOCKED`, `DONE`.

Parallel work must have explicit non-conflicting ownership/write boundaries or a defined integration point.

## Current executable slice

`WS-1`

Acceptance:

- <observable acceptance criterion>

Validation:

- `<targeted command or evidence>`

## Integration points

- <contract/merge point between parallel slices>

## Durable documentation destinations

- `docs/product.md`: <only if durable mission/users/problems/value/boundaries/principles/quality promise changes>
- `docs/architecture.md`: <only if architecture/ownership changes>
- `docs/features/<feature>.md`: <only durable current behavior>
- `docs/adr/<adr>.md`: <only if a material durable decision is made>
- tests/contracts: <executable truth>

## Completion

The workstream is complete only when applicable product intent, code, integration, failure/resource behavior, validation/evidence and durable docs agree. Then update `docs/current-state.md` and delete this file by default.
