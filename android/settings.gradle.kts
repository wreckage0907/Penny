pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "penny"
include(":app")

// Install the git hooks automatically on any Gradle run, so every clone gets
// the pre-commit checks without manual setup (pattern borrowed from moly).
// The git repo root is the parent of android/.
if (File(rootDir.parentFile, ".git").exists()) {
    ProcessBuilder("git", "config", "core.hooksPath", ".githooks")
        .directory(rootDir.parentFile)
        .start()
        .waitFor()
}
