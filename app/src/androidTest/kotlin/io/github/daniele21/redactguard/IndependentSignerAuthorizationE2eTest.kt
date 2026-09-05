package io.github.daniele21.redactguard

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daniele21.localllm.transport.binder.client.SharedRuntimeConnectionState
import io.github.daniele21.redactguard.domain.analysis.LocalAiRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Cross-APK proof for the production topology where RedactGuard and Harnex have different signers. */
@RunWith(AndroidJUnit4::class)
class IndependentSignerAuthorizationE2eTest {
    @Test
    fun bindCapabilityIsGrantedWhenConsumerWasInstalledBeforeHost() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(
            "The normal bind capability must resolve after Harnex is installed even when RedactGuard was installed first",
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(BuildConfig.SHARED_RUNTIME_PERMISSION),
        )
    }

    @Test
    fun independentSignerIsDeniedUntilExplicitlyAuthorized() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = ProcessLocalProductAnalysisOwner.get(context)

        owner.setConnectionEnabled(true)

        assertTrue(
            "Expected Harnex to deny the observed independent signer before explicit authorization",
            await(CONNECTION_TIMEOUT_MILLIS) {
                owner.connectionState.value == LocalAiRuntimeState.PERMISSION_DENIED
            },
        )
        assertEquals(
            SharedRuntimeConnectionState.PERMISSION_DENIED,
            owner.runtime.connectionSnapshot.state,
        )
    }

    @Test
    fun authorizedIndependentSignerCanDisconnectAndReconnect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = ProcessLocalProductAnalysisOwner.get(context)

        owner.setConnectionEnabled(true)
        assertTrue(
            "Expected authorized RedactGuard signer to establish the Binder transport",
            await(CONNECTION_TIMEOUT_MILLIS) {
                owner.runtime.connectionSnapshot.state == SharedRuntimeConnectionState.CONNECTED
            },
        )

        owner.setConnectionEnabled(false)
        assertFalse(owner.connectionEnabled.value)
        assertTrue(
            "Explicit disconnect must detach the Binder transport",
            await(CONNECTION_TIMEOUT_MILLIS) {
                owner.runtime.connectionSnapshot.state == SharedRuntimeConnectionState.DISCONNECTED
            },
        )

        owner.setConnectionEnabled(true)
        assertTrue(owner.connectionEnabled.value)
        assertTrue(
            "A user-requested reconnect must reuse the same Consumer SDK client",
            await(CONNECTION_TIMEOUT_MILLIS) {
                owner.runtime.connectionSnapshot.state == SharedRuntimeConnectionState.CONNECTED
            },
        )
    }

    @Test
    fun explicitDisconnectPersistsUserPreference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = ProcessLocalProductAnalysisOwner.get(context)

        owner.setConnectionEnabled(false)

        assertFalse(owner.connectionEnabled.value)
        assertTrue(
            await(CONNECTION_TIMEOUT_MILLIS) {
                owner.runtime.connectionSnapshot.state == SharedRuntimeConnectionState.DISCONNECTED
            },
        )
    }

    @Test
    fun persistedDisconnectSuppressesAutomaticReconnectUntilUserConnects() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = ProcessLocalProductAnalysisOwner.get(context)

        Thread.sleep(RECONNECT_GUARD_MILLIS)
        assertFalse(owner.connectionEnabled.value)
        assertEquals(
            SharedRuntimeConnectionState.DISCONNECTED,
            owner.runtime.connectionSnapshot.state,
        )

        owner.setConnectionEnabled(true)
        assertTrue(
            "Explicit Connect must restore the transport after a persisted opt-out",
            await(CONNECTION_TIMEOUT_MILLIS) {
                owner.runtime.connectionSnapshot.state == SharedRuntimeConnectionState.CONNECTED
            },
        )
    }

    private fun await(
        timeoutMillis: Long,
        predicate: () -> Boolean,
    ): Boolean {
        val deadline = System.nanoTime() + timeoutMillis * NANOS_PER_MILLI
        while (System.nanoTime() < deadline) {
            if (predicate()) return true
            Thread.sleep(POLL_MILLIS)
        }
        return predicate()
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLIS = 10_000L
        const val RECONNECT_GUARD_MILLIS = 750L
        const val POLL_MILLIS = 100L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
