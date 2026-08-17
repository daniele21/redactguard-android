# Product flow UI states

Status: migration in progress
Owner: RedactGuard

The RedactGuard UI explicitly represents import, definition selection, local analysis, review, zero-findings, export, success and failure states. These composables contain no Local AI, PDF or persistence logic; they render product-owned state and dispatch user intent only.

Custom PII entry remains local to the dialog until submission. Validation failures are projected with content-free guidance and do not log or persist label, definition or example values.

Successful export is presented as `PDF protetto creato` but does not itself prove redaction correctness. The physical evidence gate independently reopens the destination PDF and verifies accepted synthetic PII is absent while ignored content remains present.

Failure screens receive only content-free product messages. Document text, finding surfaces and reveal state must not be interpolated into failure strings.
