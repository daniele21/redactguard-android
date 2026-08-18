# Failure and recovery evidence matrix

Status: active gate
Document type: evidence-contract
Owner: redactguard-android
Canonical scope: product.failure-recovery
Read when: validating RedactGuard failure classification, user recovery and diagnostic identity

This matrix is the evidence contract for `docs/workstreams/failure-diagnostics-hardening.md`. A failure-path claim is green only when the expected stable code, user state, recovery behavior and privacy-safe diagnostic event are demonstrated at the narrowest boundary that can prove the claim.

Physical-device rows must be recorded against exact RedactGuard/Harness APK, signer, source and device identity as required by `docs/evidence/physical-two-apk.md`.

## Evidence rules

For every exercised failure:

1. record the stable `RG-*` code;
2. record the product stage and visible title;
3. confirm the user is told the relevant cause/constraint when RedactGuard knows it;
4. confirm the recovery action is available or the state intentionally requires a new document;
5. expand `Dettagli tecnici` and verify code, cause and stage match the failure;
6. where an operation identity exists, verify it correlates the product error with diagnostics without exposing document content;
7. confirm no document text, filename, prompt, model output, finding value, email, phone number or other PII appears in diagnostic metadata;
8. confirm failed/cancelled operations leave no unintended persistent document/review state or partial successful output.

Do not use production or client PII. Use deterministic synthetic fixtures only.

## PDF/import failures

| Cause | Stable code | Expected product title | Recovery | Minimum evidence |
| --- | --- | --- | --- | --- |
| source no longer resolvable | `RG-PDF-001` | PDF non più disponibile | reselect document | mapper/unit + targeted device/SAF scenario |
| source unreadable / permission failure | `RG-PDF-002` | Impossibile leggere il PDF | reselect/check access | mapper/unit + device permission/source scenario |
| encrypted PDF | `RG-PDF-003` | PDF protetto da password | remove protection | parser fixture + product UI/device evidence |
| malformed PDF | `RG-PDF-004` | PDF non valido | use valid copy | parser fixture + product UI/device evidence |
| unexpected parser failure | `RG-PDF-005` | Impossibile elaborare il PDF | retry/reselect | injected/parser-boundary failure + UI evidence |
| page/character bound exceeded | `RG-PDF-006` | PDF oltre i limiti supportati | smaller document | deterministic bound fixture + UI evidence |
| zero-page/empty PDF | `RG-PDF-007` | PDF vuoto | select another PDF | extractor test + UI evidence |
| image-only/no extractable text | `RG-PDF-008` | PDF senza testo estraibile | use text PDF; OCR is not supported | image-only fixture + physical product evidence |

`RG-PDF-008` is the regression anchor for the issue that triggered this workstream. It must never be projected as an omnibus message such as “cifrato, non valido, troppo grande o senza testo”.

## Harness and analysis failures

| Cause | Stable code | Expected product title | Recovery | Minimum evidence |
| --- | --- | --- | --- | --- |
| Harness not installed | `RG-AI-001` | Harness non installato | install Harness | physical two-APK absent/recovery path |
| Harness/model unavailable | `RG-AI-002` | Harness non disponibile | open/make Harness ready | contract test + physical recovery |
| permission denied | `RG-AI-003` | Accesso a Harness negato | update/fix authorization | contract test + representative device evidence |
| capability/protocol incompatible | `RG-AI-004` | Harness incompatibile | update Harness | contract test + representative incompatible fixture |
| analysis plan rejected | `RG-AI-005` | Documento non analizzabile con questi limiti | new/supported document | planner/analysis test + product projection |
| invalid structured model result | `RG-AI-006` | Risposta AI non valida | retry analysis | malformed structured-output fixture + atomic-result assertion |
| invalid findings | `RG-AI-007` | Risultati AI non validi | retry analysis | finding-validation fixture + atomic-result assertion |
| chunk execution failure | `RG-AI-008` | Analisi non completata | retry analysis | multi-chunk failure fixture + no-partial-result evidence |
| disconnect/host death | `RG-AI-009` | Connessione con Harness interrotta | reconnect/retry | physical host-death/restart path |
| cancellation | `RG-AI-010` | Analisi annullata when represented as failure | no automatic retry | cancellation lifecycle test + physical active-generation cancellation |

For analysis failures, partial findings must not become reviewable. The recorded operation ID must remain the identity of the failed analysis rather than a newly invented diagnostic-only identity.

## Review failures

| Cause | Stable code | Expected product title | Recovery | Minimum evidence |
| --- | --- | --- | --- | --- |
| pending decision blocks export | `RG-REV-001` | Revisione incompleta | complete review | redaction-plan test |
| unknown source segment | `RG-REV-002` | Revisione non valida | re-analyze | redaction-plan test |
| missing PII definition | `RG-REV-003` | Revisione non valida | re-analyze | plan/projection test |
| source text mismatch | `RG-REV-004` | Revisione non valida | re-analyze | redaction-plan test |
| duplicate occurrence | `RG-REV-005` | Revisione non valida | re-analyze | plan/projection test |
| overlap conflict | `RG-REV-006` | Revisione non valida | re-analyze | redaction-plan test |
| duplicate definition in projection | `RG-REV-007` | Revisione non valida | re-analyze | projection test |
| reveal references unknown occurrence | `RG-REV-008` | Revisione non valida | re-analyze | projection test |

Review failures may share concise user-facing copy where the actionable recovery is genuinely identical, but their stable diagnostic cause must remain distinct.

## Export failures

| Cause | Stable code | Expected product title | Recovery | Minimum evidence |
| --- | --- | --- | --- | --- |
| destination cannot be written | `RG-EXP-001` | Destinazione non scrivibile | choose another destination | exporter test + physical failed-destination scenario |
| source content mismatch | `RG-EXP-002` | Documento cambiato | re-analyze | exporter integrity test |
| output bound exceeded | `RG-EXP-003` | PDF protetto troppo grande | smaller/new document | exporter bound test |
| writer failure | `RG-EXP-004` | Esportazione non riuscita | retry export | injected writer failure + partial-output cleanup assertion |

Every export failure must remain fail-closed: no success state and no partial output that can be mistaken for a valid protected PDF.

## Internal fallback

`RG-SYS-001 UNKNOWN_INTERNAL` exists only for genuinely unclassified internal faults. It is not a convenience bucket for known PDF, Harness, analysis, review or export causes.

Any production occurrence of `RG-SYS-001` should trigger engineering review. If the underlying cause is deterministic and meaningful to product/recovery behavior, introduce a specific stable cause instead of repeatedly accepting the fallback.

## Physical evidence record

For each physical run, append or attach an identity-bearing record containing:

```text
RedactGuard commit:
RedactGuard APK SHA-256:
Harness commit:
Harness APK SHA-256:
consumer-android version:
RedactGuard signer SHA-256:
Harness signer SHA-256:
device manufacturer/model:
Android version:
device serial/asset label:
run timestamp/time zone:
synthetic fixture identity/SHA-256:

Scenario:
Expected RG code:
Observed RG code:
Expected user title:
Observed user title:
Recovery exercised:
Technical details checked:
Privacy-safe diagnostics checked:
Cleanup checked:
Result: PASS / FAIL
Evidence artifact references:
```

Screenshots/videos/log extracts must use bounded retention and synthetic data. Evidence must never include real client documents or sensitive production payloads.

## Gate completion

FD-8 is green only when the representative physical scenarios in `physical-two-apk.md` and the high-risk rows above are recorded against exact build/device identity. Repository CI alone cannot satisfy physical Host death/recovery, SAF permission behavior, Android process recreation or independent exported-PDF verification.
