package io.github.daniele21.redactguard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import kotlinx.coroutines.flow.StateFlow

/** Thin UI controller over the process-local connection owner; it does not duplicate Binder state. */
internal class HarnexConnectionSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val owner = ProcessLocalProductAnalysisOwner.get(application)

    val connectionEnabled: StateFlow<Boolean> = owner.connectionEnabled
    val connectionState: StateFlow<LocalAiRuntimeState> = owner.connectionState

    fun connect() = owner.setConnectionEnabled(true)

    fun disconnect() = owner.setConnectionEnabled(false)

    fun retry() = owner.connect()
}
