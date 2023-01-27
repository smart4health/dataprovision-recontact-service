import com.healthmetrix.recontact.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.recontact.buildlogic.conventions.exclusionsSpringTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)
    implementation(projects.persistence.requestApi)
    implementation(projects.persistence.messageApi)
    implementation(projects.jiraApi)

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.framework.web)
    implementation(libs.spring.framework.context)
    implementation(libs.spring.framework.tx)

    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
    testImplementation(projects.commonsTest)
}
