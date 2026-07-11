plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

base { archivesName = "stacktale-intellij" } // distributable is stacktale-intellij-<version>.zip

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    implementation(project(":core")) // bundled into the plugin distribution
    intellijPlatform {
        // -PlocalIdePath="C:/Program Files/JetBrains/IntelliJ IDEA 2024.3.5" builds against an
        // installed IDE (no ~1GB SDK download); the default downloads Community for CI/others.
        val localIde = providers.gradleProperty("localIdePath").orNull
        if (localIde != null) local(localIde) else intellijIdeaCommunity("2024.3.5")
    }
}

intellijPlatform {
    buildSearchableOptions = false
    instrumentCode = false // no GUI forms / NotNull weaving to do
    pluginConfiguration {
        // authoritative compat range — a local() build otherwise pins since-build to the
        // installed IDE's build number, locking the plugin to that release and up
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "261.*"
        }
    }
}

// No test sources live here (the parser is tested in :core). Disable the platform test
// task, which also sidesteps its sandbox setup being finicky against some local IDE builds.
tasks.test {
    enabled = false
}
