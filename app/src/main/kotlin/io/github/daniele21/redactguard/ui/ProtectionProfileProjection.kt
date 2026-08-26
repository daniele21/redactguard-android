package io.github.daniele21.redactguard.ui

import io.github.daniele21.redactguard.domain.pii.PiiProfileId
import io.github.daniele21.redactguard.domain.pii.PiiTypeId
import io.github.daniele21.redactguard.domain.pii.RedactGuardPiiProfiles

internal object ProtectionProfileProjector {
    fun project(definitions: List<DefinitionChoice>): List<ProtectionProfileChoice> {
        val selectedIds =
            definitions
                .asSequence()
                .filter(DefinitionChoice::selected)
                .mapNotNull { choice -> runCatching { PiiTypeId.parse(choice.id) }.getOrNull() }
                .toSet()
        val matching = RedactGuardPiiProfiles.matchingProfile(selectedIds)
        return RedactGuardPiiProfiles.all.map { profile ->
            ProtectionProfileChoice(
                id = profile.id.name,
                label = profile.label,
                description = profile.description,
                selected = profile.id == matching,
            )
        }
    }
}

/** Computes the minimal category toggles needed to make the requested product profile exact. */
internal object ProtectionProfileSelection {
    fun togglesFor(
        profileId: String,
        definitions: List<DefinitionChoice>,
    ): List<String> {
        val id = runCatching { PiiProfileId.valueOf(profileId) }.getOrNull() ?: return emptyList()
        val target =
            RedactGuardPiiProfiles.byId[id]
                ?.typeIds
                ?.map(PiiTypeId::value)
                ?.toSet() ?: return emptyList()
        val available = definitions.map(DefinitionChoice::id).toSet()
        val selected = definitions.filter(DefinitionChoice::selected).map(DefinitionChoice::id).toSet()
        val effectiveTarget = target.intersect(available)
        return definitions
            .map(DefinitionChoice::id)
            .filter { definitionId -> (definitionId in selected) != (definitionId in effectiveTarget) }
    }
}
