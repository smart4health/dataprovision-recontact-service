@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        gradlePluginPortal()
    }

    versionCatalogs {
        // naming this "libs" works, but IntelliJ is not happy about it
        create("mainLibs") {
            from(files("../libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include("conventions")
include("local-driver")
