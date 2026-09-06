---
name: remote-preflight
description: Reuse trusted equivalent RedactGuard evidence first, then run only missing deterministic gates.
---
# Remote Preflight
Resolve exact PR head/tree, live base, risks/gates/profile and material E2E identity. Reuse sufficient exact-head evidence before dispatching `/preflight auto` or a justified stronger profile. Rerun only missing/stale/invalidated proof.

Never ask the user to run automatable Android gates. Post-merge tree-equivalent reuse requires identical final tree, validated target base, sufficient gates/profile/E2E identity and trusted current evidence; release validates normally. Report source identity, gate reasons/status, evidence and remaining `REAL_ENVIRONMENT` release obligations without converting deferred proof into PASS.
