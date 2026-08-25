package io.github.daniele21.redactguard.domain.pii

/** Product-owned PII bundles. These are not Harness runtime/inference presets. */
internal enum class PiiProfileId {
    GENERAL,
    HEALTHCARE,
    FINANCIAL,
    LEGAL,
}

internal data class PiiProfile(
    val id: PiiProfileId,
    val label: String,
    val description: String,
    val typeIds: Set<PiiTypeId>,
) {
    init {
        require(label.isNotBlank())
        require(description.isNotBlank())
        require(typeIds.isNotEmpty())
    }
}

internal object RedactGuardPiiProfiles {
    const val VERSION = 1

    val all: List<PiiProfile> =
        listOf(
            profile(
                id = PiiProfileId.GENERAL,
                label = "Generale",
                description = "Identità, contatti e informazioni personali comuni.",
                ids =
                    listOf(
                        "full-name",
                        "email",
                        "telephone",
                        "postal-address",
                        "italian-tax-code",
                        "private-date",
                        "private-url",
                    ),
            ),
            profile(
                id = PiiProfileId.HEALTHCARE,
                label = "Sanitario",
                description = "Dati personali, condizioni, trattamenti, esami e misurazioni sanitarie.",
                ids =
                    listOf(
                        "full-name",
                        "email",
                        "telephone",
                        "postal-address",
                        "private-date",
                        "personal-demographic",
                        "health-condition",
                        "health-treatment",
                        "health-lab-result",
                        "personal-measurement",
                    ),
            ),
            profile(
                id = PiiProfileId.FINANCIAL,
                label = "Finanziario",
                description = "Identità, contatti, conti, IBAN e credenziali finanziarie sensibili.",
                ids =
                    listOf(
                        "full-name",
                        "email",
                        "telephone",
                        "postal-address",
                        "italian-tax-code",
                        "private-date",
                        "private-url",
                        "iban",
                        "account-number",
                        "personal-demographic",
                        "secret",
                    ),
            ),
            profile(
                id = PiiProfileId.LEGAL,
                label = "Legale",
                description = "Informazioni personali e sensibili tipiche di atti, pratiche e fascicoli legali.",
                ids =
                    listOf(
                        "full-name",
                        "email",
                        "telephone",
                        "postal-address",
                        "private-date",
                        "private-url",
                        "personal-demographic",
                        "account-number",
                        "secret",
                        "health-condition",
                        "lifestyle-info",
                    ),
            ),
        )

    val byId: Map<PiiProfileId, PiiProfile> = all.associateBy(PiiProfile::id)

    init {
        check(byId.size == all.size)
        val knownIds = RedactGuardBuiltInPiiDefinitions.byId.keys
        check(all.all { profile -> profile.typeIds.all(knownIds::contains) })
    }

    fun matchingProfile(selectedIds: Set<PiiTypeId>): PiiProfileId? =
        all.firstOrNull { profile -> profile.typeIds == selectedIds }?.id

    private fun profile(
        id: PiiProfileId,
        label: String,
        description: String,
        ids: List<String>,
    ) = PiiProfile(
        id = id,
        label = label,
        description = description,
        typeIds = ids.mapTo(linkedSetOf(), PiiTypeId::parse),
    )
}
