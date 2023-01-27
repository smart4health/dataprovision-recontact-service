@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)
    implementation(projects.persistence.requestApi)
    implementation(projects.persistence.messageApi)

    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.jdbi.core)
    implementation(libs.jdbi.postgres)
    implementation(libs.jdbi.kotlin)
    implementation(libs.jdbi.sqlobject.kotlin)

    runtimeOnly(libs.postgres)
    runtimeOnly(libs.bundles.liquibase)
}
