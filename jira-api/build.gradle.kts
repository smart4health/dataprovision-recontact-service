import com.healthmetrix.recontact.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.recontact.buildlogic.conventions.exclusionsSpringTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)
    implementation(libs.spring.boot.starter.webflux)

    implementation(libs.lazysodium)

    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
    testImplementation(projects.commonsTest)
}
