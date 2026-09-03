---
name: remote-preflight
description: Reuse trusted equivalent validation evidence first, then execute only missing deterministic integration/release gates.
---

# Remote Preflight

Use after `preflight-change` identifies `REMOTE_AUTOMATED` gates at INTEGRATION or RELEASE.

## Candidate preflight

1. Read `.engineering/commands.json`; resolve exact PR head, live base, source tree, risks, gates/profile and relevant E2E identity.
2. Search successful **exact-head** evidence first.
3. If it is sufficient, report the source run without starting another heavy Validate.
4. Otherwise dispatch `/preflight auto` or a justified stronger profile once.
5. On failure classify `CHANGE_REGRESSION`, `BASELINE_FAILURE`, `ENVIRONMENT`, `FLAKY`, `BASE_DRIFT` or `ASSUMPTION`, repair the owner and reselect gates.
6. Rerun only missing/stale/insufficient proof.

Do not ask the user to execute automatable Gradle/AndroidTest/R8 gates. Do not downgrade/suppress legitimate privacy, security or contract evidence for speed.

## Post-merge equivalence

Exact-head remains the rule for the integration candidate. After a content-preserving squash/rebase to `dev`, repository CI may reuse that green proof under a new commit SHA only when:

- the final Git tree exactly matches the validated candidate tree;
- the push base exactly matches the candidate target/base;
- required gates/profile and material E2E identity remain sufficient;
- the evidence artifact is trusted and current.

Moved base, changed tree, broader gates, direct push without evidence or RELEASE must validate normally. Report this truthfully as `tree-equivalent` reuse; do not imply the earlier run executed on the new commit object.

Preserve trusted-requester, same-repository, exact-head pinning for new runs, least privilege and secret-free execution.

Report stage, head/tree/base, risks, required gates, exact-head or tree-equivalent reused evidence, new runs, failures and remaining `REAL_ENVIRONMENT` gaps.
