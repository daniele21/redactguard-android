# Review presentation

Status: migration in progress
Owner: RedactGuard

Review presentation is hidden by default. Product-domain `ReviewOccurrence` values remain process-local; the UI projection contains only content-free occurrence identity, a safe category label, deterministic placeholder and decision unless one exact occurrence has been explicitly revealed.

An explicit reveal request may place the selected occurrence surface into `ReviewFindingModel.revealedValue`. Every other candidate remains hidden. Navigation/state owners must clear the reveal selection when moving between findings, resetting the task, exporting or leaving Review.

Placeholders are generated in deterministic source order and use the same bounded collision-safe type keys as redaction planning. A definition label that itself contains any sensitive occurrence surface is replaced with the generic label `Dato personale` before it reaches the UI.

Export eligibility remains fail-closed: every review occurrence must have a non-pending decision. Binder/runtime types and PDF concerns do not enter this presentation boundary.
