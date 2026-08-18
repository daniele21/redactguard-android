package io.github.daniele21.redactguard.domain.failure

internal enum class FailureStage {
    IMPORT,
    PARSE,
    CONNECTION,
    ANALYSIS,
    REVIEW,
    EXPORT,
    SYSTEM,
}

internal enum class FailureCategory {
    INPUT,
    DEPENDENCY,
    LIMIT,
    PROTOCOL,
    INTEGRITY,
    LIFECYCLE,
    INTERNAL,
}

internal enum class FailureRecoveryAction {
    RESELECT_DOCUMENT,
    REMOVE_PDF_PROTECTION,
    USE_VALID_PDF,
    USE_TEXT_PDF,
    USE_SMALLER_DOCUMENT,
    INSTALL_HARNESS,
    OPEN_HARNESS,
    UPDATE_HARNESS,
    RECONNECT_HARNESS,
    RETRY_ANALYSIS,
    COMPLETE_REVIEW,
    SELECT_EXPORT_DESTINATION,
    RETRY_EXPORT,
    START_NEW_DOCUMENT,
    NONE,
}

/**
 * Canonical product-owned failure registry.
 *
 * Stable codes are product API: once released they must not be reused for a different cause.
 * Infrastructure exception names are intentionally absent from this contract.
 */
internal enum class ProductFailureKind(
    val stableCode: String,
    val stage: FailureStage,
    val category: FailureCategory,
    val retryable: Boolean,
    val recoveryAction: FailureRecoveryAction,
) {
    SOURCE_NOT_FOUND("RG-PDF-001", FailureStage.IMPORT, FailureCategory.INPUT, false, FailureRecoveryAction.RESELECT_DOCUMENT),
    SOURCE_UNREADABLE("RG-PDF-002", FailureStage.IMPORT, FailureCategory.INPUT, false, FailureRecoveryAction.RESELECT_DOCUMENT),
    ENCRYPTED_PDF("RG-PDF-003", FailureStage.PARSE, FailureCategory.INPUT, false, FailureRecoveryAction.REMOVE_PDF_PROTECTION),
    MALFORMED_PDF("RG-PDF-004", FailureStage.PARSE, FailureCategory.INPUT, false, FailureRecoveryAction.USE_VALID_PDF),
    PARSER_FAILED("RG-PDF-005", FailureStage.PARSE, FailureCategory.INTERNAL, true, FailureRecoveryAction.RESELECT_DOCUMENT),
    LIMIT_EXCEEDED("RG-PDF-006", FailureStage.PARSE, FailureCategory.LIMIT, false, FailureRecoveryAction.USE_SMALLER_DOCUMENT),
    EMPTY_PDF("RG-PDF-007", FailureStage.PARSE, FailureCategory.INPUT, false, FailureRecoveryAction.RESELECT_DOCUMENT),
    IMAGE_ONLY_PDF("RG-PDF-008", FailureStage.PARSE, FailureCategory.INPUT, false, FailureRecoveryAction.USE_TEXT_PDF),

    HOST_NOT_INSTALLED("RG-AI-001", FailureStage.CONNECTION, FailureCategory.DEPENDENCY, false, FailureRecoveryAction.INSTALL_HARNESS),
    HOST_UNAVAILABLE("RG-AI-002", FailureStage.CONNECTION, FailureCategory.DEPENDENCY, true, FailureRecoveryAction.OPEN_HARNESS),
    PERMISSION_DENIED("RG-AI-003", FailureStage.CONNECTION, FailureCategory.PROTOCOL, false, FailureRecoveryAction.UPDATE_HARNESS),
    CAPABILITY_INCOMPATIBLE("RG-AI-004", FailureStage.CONNECTION, FailureCategory.PROTOCOL, false, FailureRecoveryAction.UPDATE_HARNESS),
    PLAN_REJECTED("RG-AI-005", FailureStage.ANALYSIS, FailureCategory.LIMIT, false, FailureRecoveryAction.START_NEW_DOCUMENT),
    INVALID_STRUCTURED_RESULT("RG-AI-006", FailureStage.ANALYSIS, FailureCategory.PROTOCOL, true, FailureRecoveryAction.RETRY_ANALYSIS),
    INVALID_FINDINGS("RG-AI-007", FailureStage.ANALYSIS, FailureCategory.INTEGRITY, true, FailureRecoveryAction.RETRY_ANALYSIS),
    CHUNK_FAILED("RG-AI-008", FailureStage.ANALYSIS, FailureCategory.DEPENDENCY, true, FailureRecoveryAction.RETRY_ANALYSIS),
    DISCONNECTED("RG-AI-009", FailureStage.ANALYSIS, FailureCategory.DEPENDENCY, true, FailureRecoveryAction.RECONNECT_HARNESS),
    CANCELLED("RG-AI-010", FailureStage.ANALYSIS, FailureCategory.LIFECYCLE, false, FailureRecoveryAction.NONE),

    REVIEW_PENDING_DECISION("RG-REV-001", FailureStage.REVIEW, FailureCategory.LIFECYCLE, false, FailureRecoveryAction.COMPLETE_REVIEW),
    REVIEW_UNKNOWN_SEGMENT("RG-REV-002", FailureStage.REVIEW, FailureCategory.INTEGRITY, true, FailureRecoveryAction.RETRY_ANALYSIS),
    REVIEW_MISSING_DEFINITION("RG-REV-003", FailureStage.REVIEW, FailureCategory.INTEGRITY, true, FailureRecoveryAction.RETRY_ANALYSIS),
    REVIEW_SOURCE_MISMATCH("RG-REV-004", FailureStage.REVIEW, FailureCategory.INTEGRITY, true, FailureRecoveryAction.RETRY_ANALYSIS),
    REVIEW_DUPLICATE_OCCURRENCE("RG-REV-005", FailureStage.REVIEW, FailureCategory.INTEGRITY, true, FailureRecoveryAction.RETRY_ANALYSIS),
    REVIEW_OVERLAP_CONFLICT("RG-REV-006", FailureStage.REVIEW, FailureCategory.INTEGRITY, true, FailureRecoveryAction.RETRY_ANALYSIS),

    DESTINATION_UNWRITABLE("RG-EXP-001", FailureStage.EXPORT, FailureCategory.INPUT, true, FailureRecoveryAction.SELECT_EXPORT_DESTINATION),
    SOURCE_MISMATCH("RG-EXP-002", FailureStage.EXPORT, FailureCategory.INTEGRITY, true, FailureRecoveryAction.RETRY_ANALYSIS),
    OUTPUT_LIMIT_EXCEEDED("RG-EXP-003", FailureStage.EXPORT, FailureCategory.LIMIT, false, FailureRecoveryAction.START_NEW_DOCUMENT),
    WRITER_FAILED("RG-EXP-004", FailureStage.EXPORT, FailureCategory.INTERNAL, true, FailureRecoveryAction.RETRY_EXPORT),

    UNKNOWN_INTERNAL("RG-SYS-001", FailureStage.SYSTEM, FailureCategory.INTERNAL, false, FailureRecoveryAction.START_NEW_DOCUMENT),
}

internal data class ProductFailure(
    val kind: ProductFailureKind,
    val operationId: String? = null,
) {
    init {
        require(operationId == null || operationId.isNotBlank()) { "operationId must be null or non-blank" }
        require(operationId == null || operationId.length <= 96) { "operationId is too long" }
    }

    val code: String
        get() = kind.stableCode

    override fun toString(): String =
        "ProductFailure(code=$code, stage=${kind.stage}, category=${kind.category}, retryable=${kind.retryable}, " +
            "hasOperationId=${operationId != null})"
}
