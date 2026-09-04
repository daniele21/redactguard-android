package io.github.daniele21.redactguard.domain.redaction

import io.github.daniele21.redactguard.domain.document.SourceOccurrence
import io.github.daniele21.redactguard.domain.pii.PiiTypeId

/** Content-free deterministic identity for one typed source occurrence. */
internal data class OccurrenceId(
    val typeId: PiiTypeId,
    val source: SourceOccurrence,
)

internal enum class ReviewDecisionState {
    PENDING,
    ACCEPTED,
    IGNORED,
}

/** Reveal state is presentation-only and intentionally absent from this decision domain. */
internal data class ReviewOccurrence(
    val id: OccurrenceId,
    val surface: String,
    val decision: ReviewDecisionState = ReviewDecisionState.PENDING,
) {
    init {
        require(surface.isNotBlank()) { "Review occurrence surface must not be blank" }
    }

    override fun toString(): String = "ReviewOccurrence(id=$id, surface=<redacted>, decision=$decision)"

    fun accept(): ReviewOccurrence = copy(decision = ReviewDecisionState.ACCEPTED)

    fun ignore(): ReviewOccurrence = copy(decision = ReviewDecisionState.IGNORED)

    fun resetDecision(): ReviewOccurrence = copy(decision = ReviewDecisionState.PENDING)
}
