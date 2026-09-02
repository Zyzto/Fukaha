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
        assertEquals("0.4.1", AppVersion.normalize("v0.4.1"))
        assertEquals("0.4.1", AppVersion.normalize("V0.4.1"))
        assertEquals("0.4.1", AppVersion.normalize(" 0.4.1 "))
    }

    @Test
    fun comparesSemverAndPadsMissingParts() {
        assertTrue(AppVersion.isNewer("0.4.2", "0.4.1"))
        assertTrue(AppVersion.isNewer("0.5.0", "0.4.9"))
        assertTrue(AppVersion.isNewer("1.0.0", "0.9.9"))
        assertFalse(AppVersion.isNewer("0.4.1", "0.4.1"))
        assertFalse(AppVersion.isNewer("0.4.0", "0.4.1"))
        assertEquals(0, AppVersion.compare("1.0", "1.0.0"))
        assertTrue(AppVersion.isNewer("0.4.1", "0.4.1-debug"))
    }

    @Test
    fun comparesCalVerReleasesChronologically() {
        assertTrue(AppVersion.isNewer("26.09.0", "0.5.3"))
        assertTrue(AppVersion.isNewer("26.10.0", "26.09.0"))
        assertFalse(AppVersion.isNewer("26.09.0", "26.09.0"))
    }

    @Test
    fun changelogDropsFullChangelogFooterAndHeadings() {
        val notes = ChangelogFormatter.displayNotes(
            """
            ### Added
            - Paste a link
            **Full Changelog**: https://github.com/Zyzto/Fukaha/compare/v0.4.0...v0.4.1
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
        val result = AppUpdateChecker.evaluate(dto, "0.4.1")
        val available = result as UpdateCheckResult.Available
        assertEquals("0.5.0", available.release.version)
        assertTrue(available.release.changelog.contains("Update checker"))
        assertEquals(null, available.release.apkUrl)
        assertFalse(available.release.canInstallInApp)
    }

    @Test
    fun evaluatePicksFukahaApkAsset() {
        val dto = json.decodeFromString<GithubReleaseDto>(
            """
            {
              "tag_name": "v0.5.0",
              "html_url": "https://github.com/Zyzto/Fukaha/releases/tag/v0.5.0",
              "assets": [
                {
                  "name": "notes.txt",
                  "size": 12,
                  "content_type": "text/plain",
                  "browser_download_url": "https://example.com/notes.txt"
                },
                {
                  "name": "other.apk",
                  "size": 100,
                  "content_type": "application/vnd.android.package-archive",
                  "browser_download_url": "https://example.com/other.apk"
                },
                {
                  "name": "fukaha-0.5.0.apk",
                  "size": 4242424,
                  "content_type": "application/vnd.android.package-archive",
                  "browser_download_url": "https://github.com/Zyzto/Fukaha/releases/download/v0.5.0/fukaha-0.5.0.apk"
                }
              ]
            }
            """.trimIndent(),
        )
        val result = AppUpdateChecker.evaluate(dto, "0.4.2") as UpdateCheckResult.Available
        assertEquals(
            "https://github.com/Zyzto/Fukaha/releases/download/v0.5.0/fukaha-0.5.0.apk",
            result.release.apkUrl,
        )
        assertEquals("fukaha-0.5.0.apk", result.release.apkName)
        assertEquals(4242424L, result.release.apkSizeBytes)
        assertTrue(result.release.canInstallInApp)
    }

    @Test
    fun pickApkFallsBackToAnyApkName() {
        val asset = GithubApkAsset.pick(
            listOf(
                GithubAssetDto(
                    name = "checksums.txt",
                    browserDownloadUrl = "https://example.com/sum",
                ),
                GithubAssetDto(
                    name = "app.apk",
                    size = 9,
                    browserDownloadUrl = "https://example.com/app.apk",
                ),
            ),
        )
        assertEquals("https://example.com/app.apk", asset?.browserDownloadUrl)
    }

    @Test
    fun evaluateTreatsSameTagAsUpToDate() {
        val dto = GithubReleaseDto(
            tagName = "v0.4.1",
            htmlUrl = "https://github.com/Zyzto/Fukaha/releases/tag/v0.4.1",
        )
        assertEquals(UpdateCheckResult.UpToDate, AppUpdateChecker.evaluate(dto, "0.4.1"))
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
        assertTrue(AppUpdatePolicy.shouldPrompt(release, "0.4.1"))
        assertTrue(AppUpdatePolicy.shouldPrompt(release, ""))
    }
}
