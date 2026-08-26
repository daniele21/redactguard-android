package io.github.daniele21.redactguard.domain.pii

internal data class DefinitionSelectionState(
    val definitions: List<PiiDefinition> = RedactGuardBuiltInPiiDefinitions.all,
    val selectedIds: Set<PiiTypeId> = emptySet(),
) {
    val selectedDefinitions: List<PiiDefinition>
        get() = definitions.filter { it.id in selectedIds }

    val matchingProfileId: PiiProfileId?
        get() = RedactGuardPiiProfiles.matchingProfile(selectedIds)
}

/** Process-local selection/custom-definition owner with no Android or persistence dependency. */
internal class DefinitionSelectionController(
    initialState: DefinitionSelectionState = DefinitionSelectionState(),
) {
    private var current = initialState

    val state: DefinitionSelectionState
        get() = current

    fun toggle(id: PiiTypeId): DefinitionSelectionState {
        if (current.definitions.none { it.id == id }) return current
        current =
            current.copy(
                selectedIds =
                    if (id in current.selectedIds) {
                        current.selectedIds - id
                    } else {
                        current.selectedIds + id
                    },
            )
        return current
    }

    fun applyProfile(id: PiiProfileId): DefinitionSelectionState {
        val profile = RedactGuardPiiProfiles.byId[id] ?: return current
        val availableIds = current.definitions.mapTo(linkedSetOf(), PiiDefinition::id)
        current = current.copy(selectedIds = profile.typeIds.intersect(availableIds))
        return current
    }

    fun clearSelection(): DefinitionSelectionState {
        current = current.copy(selectedIds = emptySet())
        return current
    }

    fun addCustom(draft: PiiDefinitionDraft): PiiDefinitionCreationResult {
        val creation = PiiDefinitionFactory.createCustom(draft, current.definitions)
        if (creation is PiiDefinitionCreationResult.Created) {
            current =
                current.copy(
                    definitions = current.definitions + creation.definition,
                    selectedIds = current.selectedIds + creation.definition.id,
                )
        }
        return creation
    }

    fun reset(): DefinitionSelectionState {
        current = DefinitionSelectionState()
        return current
    }
}
