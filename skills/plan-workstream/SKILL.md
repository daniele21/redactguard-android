---
name: plan-workstream
description: Plan substantial RedactGuard work as observable vertical outcomes with safe parallel ownership and bounded resume checkpoints.
---
# Plan Workstream
Use only when dependencies, parallel ownership or cross-session state need a durable DAG. Plan observable outcomes, give subtasks non-conflicting write boundaries and converge early. Track only READY/ACTIVE/BLOCKED/DONE; keep cheap validation beside subtasks and integration/release proof at outcome boundaries.

For multi-session work keep one compact checkpoint in the existing plan: head/tree/base identity, confirmed facts, excluded hypotheses/evidence pointers, unresolved questions, deferred release obligations and next discriminating action. Refresh identity on resume. Delete the plan after durable truth and release obligations transfer to canonical owners.
