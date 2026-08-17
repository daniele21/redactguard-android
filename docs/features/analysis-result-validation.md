# Structured analysis result validation

Status: migration in progress
Owner: RedactGuard

Model output is untrusted. RedactGuard parses only the exact versioned JSON object shape, with bounded response/field sizes and no repair, trailing prose or coercion. Parsed findings are not reviewable until they pass exact-source validation.

Finding validation requires a selected type, a submitted analysis segment and exactly one matching surface inside that model-visible segment. Unknown types/segments, absent or ambiguous surfaces, invalid UTF-16 boundaries and overlapping validated findings fail closed.

Fragment IDs are mapped back to canonical source coordinates without searching the source text. RedactGuard reconstructs a local source index from the complete chunk plan, requires `-fNNNN` fragments to be contiguous and to concatenate exactly to the canonical segment, and then applies cumulative local offsets. This keeps source mapping deterministic even when identical text appears elsewhere in the document.

Sensitive model surfaces remain redacted from ordinary diagnostics. The Binder adapter may transport structured output, but it must delegate parsing and source validation to this product-owned domain boundary.

The migration slice is formatted by the repository Spotless policy before exact-head validation; behavior is accepted only when the normal unit/lint/debug assembly gate passes on that same head.
