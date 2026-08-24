# Repository governance

Status: active policy
Owner: redactguard-android repository administration
Read when: changing canonical branches, merge policy, required checks or preparing a release promotion

## Canonical branch roles

`dev` is the integration branch for active product development. `main` is the release/promotion branch and must not receive feature work directly.

The repository currently still reports `main` as the GitHub default branch. The target governance state is to make `dev` the default contribution target while retaining `main` as the protected release branch.

The machine-readable desired state is `.engineering/repository-policy.json`.

## Required protection

Both `dev` and `main` must reject direct feature pushes and force pushes. Changes land through pull requests with conversation resolution and an up-to-date head before merge.

Required checks once the repo-template-sw convergence is integrated:

- `Validate / android`;
- `Repository health / engineering-baseline`.

Do not require `Repository health` while its canonical dependencies are intentionally split across active adoption PRs; enable the branch rule only after the convergence head is green.

Preferred merge method is squash so the canonical branches retain focused product changes while detailed implementation history remains on PR branches/PR discussion.

## Evidence rule

The checked-in policy is **desired state**, not proof that GitHub has enforced it. Completion requires reading the live GitHub branch settings after configuration and recording that:

- `dev` is protected;
- `main` is protected;
- the required checks match the current workflow check names;
- direct/force pushes and branch deletion are blocked;
- pull-request conversation resolution is required;
- the contribution/default branch points to `dev` if repository release policy permits it.

A repository API/CLI screenshot or response may be used as bounded administrative evidence, but it must not be committed as permanent documentation unless it has independent audit value.

## Current external gate

The available repository connector can inspect repository/branch state but does not expose branch-protection/default-branch mutation. Therefore remote enforcement remains an explicit administrative gate until changed through an authorized GitHub settings/API surface. Do not report RTA-11 as complete from this file alone.

## Release relationship

Promotion from `dev` to `main` happens only after repository validation, applicable product-experience evidence and release-specific checks are green on the exact candidate. Physical-device evidence remains separate when the release claim depends on real Android/Harness behavior.
