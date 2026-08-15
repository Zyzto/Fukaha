package app.fukaha

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class AppUpdateTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun normalizeStripsVPrefix() {
        assertEquals("0.4.0", AppVersion.normalize("v0.4.0"))
        assertEquals("0.4.0", AppVersion.normalize("V0.4.0"))
        assertEquals("0.4.0", AppVersion.normalize(" 0.4.0 "))
    }

    @Test
    fun comparesSemverAndPadsMissingParts() {
        assertTrue(AppVersion.isNewer("0.4.1", "0.4.0"))
        assertTrue(AppVersion.isNewer("0.5.0", "0.4.9"))
        assertTrue(AppVersion.isNewer("1.0.0", "0.9.9"))
        assertFalse(AppVersion.isNewer("0.4.0", "0.4.0"))
        assertFalse(AppVersion.isNewer("0.3.9", "0.4.0"))
        assertEquals(0, AppVersion.compare("1.0", "1.0.0"))
        assertTrue(AppVersion.isNewer("0.4.0", "0.4.0-debug"))
    }

    @Test
    fun changelogDropsFullChangelogFooterAndHeadings() {
        val notes = ChangelogFormatter.displayNotes(
            """
            ### Added
            - Paste a link
            **Full Changelog**: https://github.com/Zyzto/Fukaha/compare/v0.3.1...v0.4.0
            """.trimIndent(),
        )
        assertTrue(notes.startsWith("Added"))
        assertTrue(notes.contains("- Paste a link"))
        assertFalse(notes.contains("Full Changelog"))
        assertFalse(notes.contains("https://github.com"))
    }

    @Test
    fun evaluateTreatsNewerTagAsAvailable() {
        val dto = json.decodeFromString<GithubReleaseDto>(
            """
            {
              "tag_name": "v0.5.0",
              "name": "v0.5.0",
              "html_url": "https://github.com/Zyzto/Fukaha/releases/tag/v0.5.0",
              "body": "### Added\n- Update checker"
            }
            """.trimIndent(),
        )
        val result = AppUpdateChecker.evaluate(dto, "0.4.0")
        val available = result as UpdateCheckResult.Available
        assertEquals("0.5.0", available.release.version)
        assertTrue(available.release.changelog.contains("Update checker"))
    }

    @Test
    fun evaluateTreatsSameTagAsUpToDate() {
        val dto = GithubReleaseDto(
            tagName = "v0.4.0",
            htmlUrl = "https://github.com/Zyzto/Fukaha/releases/tag/v0.4.0",
        )
        assertEquals(UpdateCheckResult.UpToDate, AppUpdateChecker.evaluate(dto, "0.4.0"))
    }

    @Test
    fun launchCheckIsDueAfterInterval() {
        assertTrue(AppUpdatePolicy.isLaunchCheckDue(0L, nowEpochMs = 1_000L))
        assertFalse(
            AppUpdatePolicy.isLaunchCheckDue(
                lastCheckEpochMs = 1_000L,
                nowEpochMs = 1_000L + AppUpdatePolicy.LAUNCH_CHECK_INTERVAL_MS - 1,
            ),
        )
        assertTrue(
            AppUpdatePolicy.isLaunchCheckDue(
                lastCheckEpochMs = 1_000L,
                nowEpochMs = 1_000L + AppUpdatePolicy.LAUNCH_CHECK_INTERVAL_MS,
            ),
        )
    }

    @Test
    fun skippedVersionSuppressesSameReleaseOnly() {
        val release = AppRelease(
            version = "0.5.0",
            tagName = "v0.5.0",
            title = "v0.5.0",
            changelog = "notes",
            htmlUrl = "https://example.com",
        )
        assertFalse(AppUpdatePolicy.shouldPrompt(release, "0.5.0"))
        assertTrue(AppUpdatePolicy.shouldPrompt(release, "0.4.0"))
        assertTrue(AppUpdatePolicy.shouldPrompt(release, ""))
    }
}
