---
name: finalize-workstream
description: Close completed work by transferring durable truth and deferred release obligations before deleting the temporary plan.
---
# Finalize Workstream
Confirm acceptance from code/tests/evidence, make affected canonical docs current and update `docs/current-state.md` only for integrated/blocker/next truth. Transfer every deferred physical-device/release obligation to its canonical owner before deleting the plan; a required check recorded only in the workstream keeps it open. Then remove the plan by default, search stale links/claims and run repository/docs/E2E/context validation plus relevant project tests.
