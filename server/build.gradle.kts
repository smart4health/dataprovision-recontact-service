@file:Suppress("UnstableApiUsage")

import com.healthmetrix.recontact.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.recontact.buildlogic.conventions.exclusionsSpringTestRuntime
import com.healthmetrix.recontact.buildlogic.conventions.registeringExtended

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.spring)
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {
    imageName.set("healthmetrixgmbh/recontact")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.aop)

    runtimeOnly(projects.commons)
    runtimeOnly(projects.persistence)
    runtimeOnly(projects.request)
    runtimeOnly(projects.message)

    testImplementation(projects.commonsTest)
    testImplementation(projects.commons)
    testImplementation(projects.persistence.messageApi)
    testImplementation(projects.persistence.requestApi)
    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        val acceptance by registeringExtended(test, libs.versions.junit.get()) {}
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("acceptance"))
}
