package io.github.daniele21.redactguard

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class TwoApkInferenceActivityAuditE2eTest {
    @Test
    fun completedRedactGuardInferenceRemainsVisibleAfterHostRestart() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        clearEvidence(application)

        launchHarness()
        openActivity()
        val beforeList = awaitUi("RedactGuard inference activity before restart") { xml ->
            xml.takeIf {
                it.contains("RedactGuard") &&
                    it.contains(BuildConfig.APPLICATION_ID) &&
                    it.contains("COMPLETED")
            }
        }
        assertTrue(beforeList.contains(BuildConfig.APPLICATION_ID))

        tapNode(attribute = "text", value = "RedactGuard")
        val beforeDetail = awaitUi("audited inference detail before restart") { xml ->
            xml.takeIf { activityDetailReady(it) }
        }
        val contentMarker = requireActivityEvidence(beforeDetail)
        captureScreenshot(application, "01-activity-before-host-restart")

        shell("am force-stop ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}")
        launchHarness()
        openActivity()
        val afterList = awaitUi("RedactGuard inference activity after restart") { xml ->
            xml.takeIf {
                it.contains("RedactGuard") &&
                    it.contains(BuildConfig.APPLICATION_ID) &&
                    it.contains("COMPLETED")
            }
        }
        assertTrue(afterList.contains(BuildConfig.APPLICATION_ID))

        tapNode(attribute = "text", value = "RedactGuard")
        val afterDetail = awaitUi("same audited inference detail after restart") { xml ->
            xml.takeIf { activityDetailReady(it) && it.contains(contentMarker) }
        }
        requireActivityEvidence(afterDetail)
        assertTrue(afterDetail.contains(contentMarker))
        captureScreenshot(application, "02-activity-after-host-restart")
        writeEvidence(application, contentMarker)
    }

    private fun launchHarness() {
        shell(
            "am start -W -n ${BuildConfig.SHARED_RUNTIME_HOST_PACKAGE}/" +
                "io.github.daniele21.localllm.phonetest.MainActivity",
        )
        awaitUi("Harness navigation") { xml ->
            xml.takeIf { it.contains("content-desc=\"Activity\"") }
        }
    }

    private fun openActivity() {
        tapNode(attribute = "content-desc", value = "Activity")
        awaitUi("Harness Activity screen") { xml ->
            xml.takeIf { it.contains("Local inference history") }
        }
    }

    private fun activityDetailReady(xml: String): Boolean =
        xml.contains("Inference metrics") &&
            xml.contains("Execution identity") &&
            xml.contains("COMPLETED") &&
            xml.contains(BuildConfig.APPLICATION_ID)

    private fun requireActivityEvidence(xml: String): String {
        assertTrue(xml.contains("COMPLETED"))
        assertTrue(xml.contains(BuildConfig.APPLICATION_ID))
        assertTrue(xml.contains("Inference metrics"))
        assertTrue(xml.contains("Execution identity"))
        assertTrue(xml.contains("Output tokens"))
        assertTrue(xml.contains("Decode throughput"))
        assertFalse(xml.contains("No input content recorded."))
        assertFalse(xml.contains("No answer output recorded."))
        val marker = listOf("queue@example.test", "ada@example.test").firstOrNull(xml::contains)
        assertNotNull("Expected a source-backed RedactGuard input marker", marker)
        return requireNotNull(marker)
    }

    private fun tapNode(attribute: String, value: String) {
        val bounds = awaitValue("$attribute=$value bounds") {
            nodeBounds(dumpUi(), attribute, value)
        }
        val x = (bounds.left + bounds.right) / 2
        val y = (bounds.top + bounds.bottom) / 2
        shell("input tap $x $y")
    }

    private fun nodeBounds(xml: String, attribute: String, value: String): UiBounds? {
        val escaped = Regex.escape(value)
        val node = Regex("""<node\b[^>]*$attribute="$escaped"[^>]*>""").find(xml)?.value ?: return null
        val match = BOUNDS_REGEX.find(node) ?: return null
        return UiBounds(
            left = match.groupValues[1].toInt(),
            top = match.groupValues[2].toInt(),
            right = match.groupValues[3].toInt(),
            bottom = match.groupValues[4].toInt(),
        )
    }

    private fun dumpUi(): String {
        shell("uiautomator dump $UI_DUMP_PATH >/dev/null 2>&1")
        return shell("cat $UI_DUMP_PATH")
    }

    private fun awaitUi(
        label: String,
        producer: (String) -> String?,
    ): String = awaitValue(label) { producer(dumpUi()) }

    private fun <T : Any> awaitValue(
        label: String,
        timeoutMs: Long = UI_TIMEOUT_MS,
        producer: () -> T?,
    ): T {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            producer()?.let { return it }
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError("Timed out waiting for $label; ui=${dumpUi()}")
    }

    private fun captureScreenshot(application: Application, name: String) {
        val directory = evidenceDirectory(application).apply(File::mkdirs)
        val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        FileOutputStream(File(directory, "$name.png")).use { output ->
            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        screenshot.recycle()
    }

    private fun writeEvidence(application: Application, contentMarker: String) {
        val directory = evidenceDirectory(application).apply(File::mkdirs)
        File(directory, "inference-activity-identity.txt").writeText(
            buildString {
                appendLine("verified_package=${BuildConfig.APPLICATION_ID}")
                appendLine("content_marker=$contentMarker")
                appendLine("terminal_status=COMPLETED")
                appendLine("metrics_visible=true")
                appendLine("survived_host_restart=true")
            },
        )
    }

    private fun clearEvidence(application: Application) {
        evidenceDirectory(application).deleteRecursively()
    }

    private fun evidenceDirectory(application: Application): File =
        File(requireNotNull(application.getExternalFilesDir(null)), "two-apk-inference-activity")

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return descriptor.use { pfd ->
            FileInputStream(pfd.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }

    private data class UiBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )

    private companion object {
        val BOUNDS_REGEX = Regex("""bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]""")
        const val UI_DUMP_PATH = "/sdcard/Download/harnex-inference-activity.xml"
        const val POLL_INTERVAL_MS = 100L
        const val UI_TIMEOUT_MS = 15_000L
    }
}
