import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

val webAppVersionSource = layout.projectDirectory.file(
    "src/jsMain/kotlin/app/fukaha/web/WebAppVersion.kt",
)
val webAppVersion = Regex("""const val WEB_APP_VERSION = "([^"]+)"""")
    .find(webAppVersionSource.asFile.readText())
    ?.groupValues
    ?.get(1)
    ?: error("WEB_APP_VERSION is missing from ${webAppVersionSource.asFile}")

version = webAppVersion

kotlin {
    js {
        browser {
            commonWebpackConfig {
                // Development keeps the stable watcher URL. A production-only webpack
                // plugin hashes the emitted bundle and rewrites index.html plus sw.js.
                outputFileName = "fukaha.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.shared)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<KotlinWebpack>().configureEach {
    if (name.contains("Production")) {
        // Production maps are neither deployed nor uploaded to an error tracker.
        // Keep development maps intact for continuous browser iteration.
        sourceMaps = false
    }
}

tasks.named("jsBrowserDistribution") {
    doFirst {
        // The Kotlin distribution task is a Copy, not a Sync; without cleanup, obsolete hashed
        // bundles survive and can be deployed alongside the current one.
        delete(outputs.files.singleFile)
    }
    doLast {
        // Kotlin places resources into the distribution after webpack, so rewrite the final
        // copies rather than assuming index.html and sw.js are webpack compilation assets.
        val outputDirectory = outputs.files.singleFile
        val bundle = outputDirectory.listFiles()
            .orEmpty()
            .singleOrNull { it.name.matches(Regex("""fukaha\.[0-9a-f]{12}\.js""")) }
            ?: error("Could not find exactly one content-hashed Fukaha production bundle")
        val bundlePath = "/${bundle.name}"
        val buildHash = bundle.name.removePrefix("fukaha.").removeSuffix(".js")

        val index = outputDirectory.resolve("index.html")
        check(index.isFile) { "Missing production shell asset: index.html" }
        index.writeText(index.readText().replace("/fukaha.js", bundlePath))
        check(bundlePath in index.readText() && "/fukaha.js" !in index.readText()) {
            "Production index did not receive the content-hashed bundle"
        }

        val serviceWorker = outputDirectory.resolve("sw.js")
        check(serviceWorker.isFile) { "Missing production shell asset: sw.js" }
        serviceWorker.writeText(
            serviceWorker.readText()
                .replace("/fukaha.js", bundlePath)
                .replace("fukaha-shell-v2", "fukaha-shell-$buildHash"),
        )
        check(bundlePath in serviceWorker.readText() && "fukaha-shell-$buildHash" in serviceWorker.readText()) {
            "Production service worker did not receive the content-hashed shell"
        }
    }
}

val pwaStaticTest by tasks.registering(Exec::class) {
    group = "verification"
    description = "Validates PWA assets, localization parity, CSS invariants, and hosting headers."
    commandLine("node", "--test", layout.projectDirectory.file("src/staticTest/pwa-static.test.mjs"))
}

tasks.named("check") {
    dependsOn(pwaStaticTest)
}
