# Editable analysis prompt settings

Status: active feature contract
Owner: RedactGuard

## User outcome

A user can understand the instructions RedactGuard gives the local AI, customize those instructions when needed, and safely return to the product default without needing Harnex or model/runtime knowledge.

## UX contract

Settings shows the current prompt as a bounded preview with an explicit `Predefinito` or `Personalizzato` state. Editing is an advanced action reached through `Visualizza e modifica`; the normal analysis journey requires no prompt configuration.

The editor is a dedicated full-screen surface on compact Android windows. It provides:

- a large multiline editor suitable for software-keyboard use;
- inline validation and a character counter;
- `Annulla` and `Salva` with no implicit autosave;
- `Ripristina predefinito` as a draft change, committed only by `Salva`;
- progressively disclosed protected integrity rules in read-only form;
- an explanation that changes apply only to future analyses;
- a warning that custom prompt text is a persistent app-private preference and should not contain document contents, passwords or secrets.

The surface must remain operable with large text, portrait/landscape, touch navigation and assistive semantics. Status and validation meaning cannot depend on color alone.

## Product/runtime contract

The editable prompt belongs to RedactGuard, not Harnex. Harnex continues to own model/preset/runtime policy; no Host administration is duplicated in Settings.

Only a custom prompt is persisted. Absence of a custom value resolves to the current built-in default. Prompt values are normalized, bounded and validated before storage; prompt contents are never included in ordinary logs or diagnostic `toString` output.

At analysis start, RedactGuard snapshots the active editable prompt. The snapshot is transformed into an effective instruction by appending mandatory protected integrity rules, then attached to every planned chunk for that job. A Settings change cannot alter a running job.

Chunk planning budgets the actual effective instruction for that job. Prompt customization therefore cannot silently exceed Harnex input limits.

## Protected integrity rules

Customization does not control the acceptance boundary. RedactGuard always requires the model to use supplied type IDs and segment IDs exactly, return only exact non-empty source substrings, omit absent selected types, and avoid placeholder/sentinel findings. Deterministic structured-result and source validation remains authoritative if the model ignores those instructions.

## Evidence

Required deterministic evidence includes:

- prompt-policy unit tests for robust default guidance and invalid custom values;
- chunk-planning tests proving the custom effective instruction is carried and budgeted;
- app-private preference instrumentation tests for custom/default normalization;
- Compose journey coverage for preview -> edit -> save -> `Personalizzato` -> draft reset -> save -> `Predefinito`;
- affected Android compile/unit/lint/static checks selected by repository policy;
- material Settings UX evidence in the repository-declared UI evidence mode before integration.

The representative real-model regression remains REAL_ENVIRONMENT evidence when it requires the production ARM64/GGUF runtime: a document containing only a subset of selected PII categories must not yield renamed type IDs or placeholder findings for absent categories.
