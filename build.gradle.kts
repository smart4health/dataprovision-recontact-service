import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.gradle.versions)

    // https://youtrack.jetbrains.com/issue/KT-30276
    alias(libs.plugins.kotlin.jvm) apply false

    // Run local tasks
    id("com.healthmetrix.kotlin.localdriver")
}

allprojects {
    group = "com.healthmetrix"
    version = "1.0-SNAPSHOT"
}

tasks.withType<DependencyUpdatesTask> {
    outputFormatter = closureOf<Result> {
        val sb = StringBuilder()
        outdated.dependencies.forEach { dep ->
            sb.append("${dep.group}:${dep.name} ${dep.version} -> ${dep.available.release ?: dep.available.milestone}\n")
        }
        if (sb.isNotBlank()) {
            rootProject.file("build/dependencyUpdates/outdated-dependencies").apply {
                parentFile.mkdirs()
                println(sb.toString())
                writeText(sb.toString())
            }
        } else {
            println("Up to date!")
        }
    }

    // no alphas, betas, milestones, release candidates
    // or whatever the heck jaxb-api is using
    rejectVersionIf {
        candidate.version.contains("alpha", ignoreCase = true) or
            candidate.version.contains("beta", ignoreCase = true) or
            candidate.version.contains(Regex("M[0-9]*$")) or
            candidate.version.contains("RC", ignoreCase = true) or
            candidate.version.contains(Regex("b[0-9]+\\.[0-9]+$")) or
            candidate.version.contains("eap")
    }
}

tasks.register<com.healthmetrix.recontact.buildlogic.localdriver.EncryptFileTask>("encryptFileTask")
tasks.register<com.healthmetrix.recontact.buildlogic.localdriver.DecryptFileTask>("decryptFileTask")
