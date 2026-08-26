package io.github.daniele21.redactguard.domain.pii

@JvmInline
internal value class PiiTypeId private constructor(
    val value: String,
) {
    companion object {
        private val valuePattern = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

        fun parse(value: String): PiiTypeId {
            require(value.length <= PiiDefinitionLimits.MAX_TYPE_ID_CHARS) { "PII type ID is too long" }
            require(valuePattern.matches(value)) { "Invalid PII type ID" }
            return PiiTypeId(value)
        }
    }
}

internal enum class PiiDefinitionSource { BUILT_IN, CUSTOM }

/** Stable semantic grouping used by product UI only; it never replaces the exact PII type ID. */
internal enum class PiiSemanticCategory {
    IDENTITY,
    CONTACT,
    LOCATION,
    DATE,
    FINANCIAL,
    HEALTH,
    LAB,
    MEASUREMENT,
    LIFESTYLE,
    SECRET,
    CUSTOM,
}

internal data class PiiDefinition(
    val id: PiiTypeId,
    val label: String,
    val definition: String,
    val example: String? = null,
    val source: PiiDefinitionSource,
    val semanticCategory: PiiSemanticCategory = PiiSemanticCategory.CUSTOM,
) {
    init {
        require(label.isNotBlank()) { "PII label must not be blank" }
        require(definition.isNotBlank()) { "PII definition must not be blank" }
        require(codePointCount(label) <= PiiDefinitionLimits.MAX_LABEL_CODE_POINTS) { "PII label is too long" }
        require(codePointCount(definition) <= PiiDefinitionLimits.MAX_DEFINITION_CODE_POINTS) { "PII definition is too long" }
        require(example == null || codePointCount(example) <= PiiDefinitionLimits.MAX_EXAMPLE_CODE_POINTS) { "PII example is too long" }
        require(!containsUnsupportedControl(label)) { "PII label contains unsupported control characters" }
        require(!containsUnsupportedControl(definition)) { "PII definition contains unsupported control characters" }
        require(example == null || !containsUnsupportedControl(example)) { "PII example contains unsupported control characters" }
        if (source == PiiDefinitionSource.BUILT_IN) {
            require(semanticCategory != PiiSemanticCategory.CUSTOM) { "Built-in PII requires a product semantic category" }
        }
    }

    override fun toString(): String =
        "PiiDefinition(id=$id, source=$source, semanticCategory=$semanticCategory, label=<redacted>, definition=<redacted>, example=<redacted>)"
}

internal data class PiiDefinitionDraft(
    val label: String,
    val definition: String,
    val example: String? = null,
) {
    override fun toString(): String = "PiiDefinitionDraft(label=<redacted>, definition=<redacted>, example=<redacted>)"
}

internal enum class PiiDefinitionIssue {
    BLANK_LABEL,
    BLANK_DEFINITION,
    LABEL_TOO_LONG,
    DEFINITION_TOO_LONG,
    EXAMPLE_TOO_LONG,
    UNSUPPORTED_CONTROL_CHARACTER,
    CUSTOM_DEFINITION_LIMIT_REACHED,
}

internal data class PiiDefinitionValidation(
    val issues: Set<PiiDefinitionIssue>,
) {
    val isValid: Boolean get() = issues.isEmpty()
}

internal sealed interface PiiDefinitionCreationResult {
    data class Created(
        val definition: PiiDefinition,
    ) : PiiDefinitionCreationResult

    data class Invalid(
        val validation: PiiDefinitionValidation,
    ) : PiiDefinitionCreationResult
}

internal object PiiDefinitionLimits {
    const val MAX_TYPE_ID_CHARS = 64
    const val MAX_LABEL_CODE_POINTS = 64
    const val MAX_DEFINITION_CODE_POINTS = 320
    const val MAX_EXAMPLE_CODE_POINTS = 160
    const val MAX_ACTIVE_DEFINITIONS = 24
    const val MAX_CUSTOM_DEFINITIONS = 6
}

internal object PiiDefinitionFactory {
    fun validateCustomDraft(
        draft: PiiDefinitionDraft,
        existingDefinitions: Collection<PiiDefinition>,
    ): PiiDefinitionValidation {
        val issues = linkedSetOf<PiiDefinitionIssue>()
        if (draft.label.isBlank()) issues += PiiDefinitionIssue.BLANK_LABEL
        if (draft.definition.isBlank()) issues += PiiDefinitionIssue.BLANK_DEFINITION
        if (codePointCount(draft.label) > PiiDefinitionLimits.MAX_LABEL_CODE_POINTS) issues += PiiDefinitionIssue.LABEL_TOO_LONG
        if (codePointCount(draft.definition) > PiiDefinitionLimits.MAX_DEFINITION_CODE_POINTS) {
            issues += PiiDefinitionIssue.DEFINITION_TOO_LONG
        }
        if (draft.example != null && codePointCount(draft.example) > PiiDefinitionLimits.MAX_EXAMPLE_CODE_POINTS) {
            issues += PiiDefinitionIssue.EXAMPLE_TOO_LONG
        }
        if (
            containsUnsupportedControl(draft.label) ||
            containsUnsupportedControl(draft.definition) ||
            draft.example?.let(::containsUnsupportedControl) == true
        ) {
            issues += PiiDefinitionIssue.UNSUPPORTED_CONTROL_CHARACTER
        }
        if (
            existingDefinitions.count { it.source == PiiDefinitionSource.CUSTOM } >= PiiDefinitionLimits.MAX_CUSTOM_DEFINITIONS ||
            existingDefinitions.size >= PiiDefinitionLimits.MAX_ACTIVE_DEFINITIONS
        ) {
            issues += PiiDefinitionIssue.CUSTOM_DEFINITION_LIMIT_REACHED
        }
        return PiiDefinitionValidation(issues)
    }

    fun createCustom(
        draft: PiiDefinitionDraft,
        existingDefinitions: Collection<PiiDefinition>,
    ): PiiDefinitionCreationResult {
        val validation = validateCustomDraft(draft, existingDefinitions)
        if (!validation.isValid) return PiiDefinitionCreationResult.Invalid(validation)
        return PiiDefinitionCreationResult.Created(
            PiiDefinition(
                id = nextCustomId(existingDefinitions.map(PiiDefinition::id).toSet()),
                label = draft.label.trim(),
                definition = draft.definition.trim(),
                example = draft.example?.trim()?.takeIf(String::isNotEmpty),
                source = PiiDefinitionSource.CUSTOM,
                semanticCategory = PiiSemanticCategory.CUSTOM,
            ),
        )
    }

    private fun nextCustomId(existingIds: Set<PiiTypeId>): PiiTypeId {
        var ordinal = 1
        while (true) {
            val candidate = PiiTypeId.parse("custom-$ordinal")
            if (candidate !in existingIds) return candidate
            ordinal += 1
        }
    }
}

internal class PiiDefinitionSet private constructor(
    definitions: List<PiiDefinition>,
) {
    val definitions: List<PiiDefinition> = definitions.toList()
    val ids: Set<PiiTypeId> = this.definitions.mapTo(linkedSetOf(), PiiDefinition::id)

    companion object {
        fun create(definitions: Collection<PiiDefinition>): Result<PiiDefinitionSet> =
            runCatching {
                require(definitions.isNotEmpty()) { "At least one PII definition is required" }
                require(definitions.size <= PiiDefinitionLimits.MAX_ACTIVE_DEFINITIONS) { "Too many active PII definitions" }
                require(definitions.count { it.source == PiiDefinitionSource.CUSTOM } <= PiiDefinitionLimits.MAX_CUSTOM_DEFINITIONS) {
                    "Too many custom PII definitions"
                }
                require(definitions.groupingBy(PiiDefinition::id).eachCount().none { it.value > 1 }) { "Duplicate PII type IDs" }
                PiiDefinitionSet(definitions.toList())
            }
    }
}

/**
 * Versioned product taxonomy. V2 preserves the six original Android IDs and adds the richer
 * RedactGuard desktop categories so existing result IDs remain stable while the product expands.
 */
internal object RedactGuardBuiltInPiiDefinitions {
    const val VERSION = 2

    val all: List<PiiDefinition> =
        listOf(
            builtIn(
                id = "full-name",
                label = "Nome completo",
                definition = "Nome, cognome o nome completo che identifica o rende identificabile una persona fisica.",
                category = PiiSemanticCategory.IDENTITY,
            ),
            builtIn(
                id = "email",
                label = "Email",
                definition = "Indirizzo email personale o direttamente riferibile a una persona fisica.",
                category = PiiSemanticCategory.CONTACT,
            ),
            builtIn(
                id = "telephone",
                label = "Telefono",
                definition = "Numero di telefono fisso o mobile personale o direttamente riferibile a una persona fisica.",
                category = PiiSemanticCategory.CONTACT,
            ),
            builtIn(
                id = "postal-address",
                label = "Indirizzo postale",
                definition = "Indirizzo di residenza, domicilio, recapito o altra localizzazione privata riferibile a una persona fisica.",
                category = PiiSemanticCategory.LOCATION,
            ),
            builtIn(
                id = "italian-tax-code",
                label = "Codice fiscale",
                definition = "Codice fiscale italiano riferibile a una persona fisica.",
                category = PiiSemanticCategory.IDENTITY,
            ),
            builtIn(
                id = "iban",
                label = "IBAN",
                definition = "Codice IBAN di un conto bancario riferibile a una persona fisica.",
                category = PiiSemanticCategory.FINANCIAL,
            ),
            builtIn(
                id = "private-date",
                label = "Data personale",
                definition =
                    "Data riferibile alla vita privata o all'identità di una persona, come nascita o altri eventi " +
                        "personali identificanti.",
                category = PiiSemanticCategory.DATE,
            ),
            builtIn(
                id = "private-url",
                label = "URL personale",
                definition = "URL, profilo o indirizzo web privato direttamente riferibile a una persona fisica.",
                category = PiiSemanticCategory.CONTACT,
            ),
            builtIn(
                id = "account-number",
                label = "Numero di conto",
                definition =
                    "Numero o identificativo di conto, carta o rapporto finanziario personale diverso da un IBAN " +
                        "già classificabile separatamente.",
                category = PiiSemanticCategory.FINANCIAL,
            ),
            builtIn(
                id = "personal-demographic",
                label = "Dato demografico",
                definition =
                    "Informazione demografica personale come età, data di nascita, genere, nazionalità, stato civile " +
                        "o composizione familiare.",
                category = PiiSemanticCategory.IDENTITY,
            ),
            builtIn(
                id = "secret",
                label = "Credenziale o segreto",
                definition = "Segreto di autenticazione o accesso come password, PIN, token, API key, recovery code o credenziale privata.",
                category = PiiSemanticCategory.SECRET,
            ),
            builtIn(
                id = "health-condition",
                label = "Condizione di salute",
                definition = "Diagnosi, patologia, sintomo, disturbo o altra condizione clinica riferibile a una persona.",
                category = PiiSemanticCategory.HEALTH,
            ),
            builtIn(
                id = "health-treatment",
                label = "Trattamento sanitario",
                definition = "Farmaco, terapia, intervento, procedura, prescrizione o altro trattamento sanitario riferibile a una persona.",
                category = PiiSemanticCategory.HEALTH,
            ),
            builtIn(
                id = "health-lab-result",
                label = "Risultato clinico o di laboratorio",
                definition = "Esito, valore o risultato di esame clinico, analisi di laboratorio o test diagnostico riferibile a una persona.",
                category = PiiSemanticCategory.LAB,
            ),
            builtIn(
                id = "personal-measurement",
                label = "Misurazione personale",
                definition = "Misurazione fisica o biometrica personale come altezza, peso, pressione, temperatura o altra misura corporea.",
                category = PiiSemanticCategory.MEASUREMENT,
            ),
            builtIn(
                id = "lifestyle-info",
                label = "Informazione sullo stile di vita",
                definition = "Informazione privata su abitudini, alimentazione, attività fisica, fumo, consumo di alcol o altri comportamenti personali.",
                category = PiiSemanticCategory.LIFESTYLE,
            ),
        )

    val byId: Map<PiiTypeId, PiiDefinition> = all.associateBy(PiiDefinition::id)

    init {
        check(PiiDefinitionSet.create(all).isSuccess)
        check(byId.size == all.size)
    }

    private fun builtIn(
        id: String,
        label: String,
        definition: String,
        category: PiiSemanticCategory,
    ) = PiiDefinition(
        id = PiiTypeId.parse(id),
        label = label,
        definition = definition,
        source = PiiDefinitionSource.BUILT_IN,
        semanticCategory = category,
    )
}

private fun codePointCount(value: String): Int = value.codePointCount(0, value.length)

private fun containsUnsupportedControl(value: String): Boolean = value.any(Character::isISOControl)
