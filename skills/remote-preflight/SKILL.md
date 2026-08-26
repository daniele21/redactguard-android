---
name: remote-preflight
description: Execute and close the narrowest sufficient deterministic validation through repository-owned remote automation when the current coding agent lacks an equivalent local execution environment, without delegating automatable test work to the user or running full CI by default.
---

# Remote Preflight

Use this Skill when `preflight-change` classifies one or more required deterministic gates as `REMOTE_AUTOMATED`.

The governing rules are:

> Do not turn the user into a CI runner because the current agent lacks a shell, checkout, SDK or platform toolchain.

> Do not turn every small PR into a full repository/release build. Select validation from the actual blast radius.

## Workflow

1. Read `.engineering/commands.json` and confirm trigger, selector, target PR/head, canonical gates, logs, timeout/retention and trust restrictions.
2. Default to `auto`: `LEAN` for docs/governance, `SCOPED` for contained app implementation, `STRONG` for Harness/Binder/privacy/persistence/manifest/dependency/R8/package changes, `FULL` for promotion/release/selector/global-build/toolchain/unknown executable scope.
3. Verify the PR base and exact current head SHA before triggering. Never reuse evidence after edits/rebases/base movement.
4. Record selected profile, reason, affected jobs and gate results.
5. On failure inspect logs, classify `CHANGE_REGRESSION`, `BASELINE_FAILURE`, `ENVIRONMENT`, `FLAKY`, `BASE_DRIFT` or `ASSUMPTION`, identify the owning invariant, patch the owner and retrigger.
6. Re-run profile selection after every material repair; a ProGuard/global Gradle fix may legitimately escalate the next run.
7. Never ask the user to execute the same automatable test between repair attempts.
8. Keep the execution job read-only/secret-free; use separate reporting permission if the PR must be updated.

Do not suppress R8/lint/tests, add broad keep rules blindly, weaken a legitimate gate or downgrade the profile to escape a failure.

## Output

```text
HEAD: <revision>
TARGET: <branch>@<revision>
REMOTE_TRIGGER: <mechanism>
VALIDATION_PROFILE: LEAN|SCOPED|STRONG|FULL
PROFILE_REASON: <reason>
AFFECTED_SCOPE: <jobs/components>
REMOTE_GATES:
  <gate>: PASS|FAIL|PENDING|N/A
FAILURE_CLASS: <class|N/A>
REAL_ENVIRONMENT:
  <gate>: PENDING|PASS|N/A
READINESS: AUTOMATED_PREFLIGHT_CONFIRMED|NOT_READY_FOR_AUTOMATED_PREFLIGHT
```
