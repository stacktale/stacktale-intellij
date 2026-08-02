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

        pluginVerifier() // the CLI the verifyPlugin task shells out to
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

    // sinceBuild above is a claim; this is what makes it true. 2024.2.5 is the oldest release
    // in the 242 line, so it is where an API the plugin uses might not exist yet — the failure
    // the compile target cannot show. untilBuild stays unverified on purpose: 261 has no
    // release to check against, so it remains a forward-looking promise.
    //
    // Use the string notation. The ide(IntelliJPlatformType, String) overload accepts these
    // versions and then schedules nothing, leaving a green run that verified one IDE.
    pluginVerification {
        ides {
            ide("IC-2024.2.5") // the floor sinceBuild = 242 claims
            ide("IC-2024.3.5") // what the plugin compiles against
        }
    }
}

// No test sources live here (the parser is tested in :core). Disable the platform test
// task, which also sidesteps its sandbox setup being finicky against some local IDE builds.
tasks.test {
    enabled = false
}
