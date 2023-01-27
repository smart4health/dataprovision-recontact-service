import com.healthmetrix.recontact.buildlogic.conventions.excludeReflect

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    api(libs.slf4j.api)
    api(libs.logback.encoder)

    api(libs.result)
    api(libs.micrometer.prometheus)

    api(libs.kotlin.reflect)
    api(libs.jackson.kotlin) { excludeReflect() }

    implementation(libs.spring.framework.web)
    implementation(libs.spring.framework.context)
    implementation(libs.aws.secretsmanager)
    implementation(libs.spring.cloud.vault.config)
}
