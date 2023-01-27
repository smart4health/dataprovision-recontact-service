package com.healthmetrix.recontact.buildlogic.conventions

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.kotlin.dsl.exclude

fun ExternalModuleDependency.exclusionsTestImplementation() {
    // none yet
}

fun ExternalModuleDependency.exclusionsTestRuntime() {
    exclude(group = "junit", module = "junit")
}

fun ExternalModuleDependency.exclusionsSpringTestImplementation() {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(group = "com.vaadin.external.google", module = "android-json")
    exclude(module = "junit")
    exclude(module = "mockito-core")
}

fun ExternalModuleDependency.exclusionsSpringTestRuntime() {
    exclude(group = "junit", module = "junit")
}

fun ExternalModuleDependency.excludeReflect() {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
}
