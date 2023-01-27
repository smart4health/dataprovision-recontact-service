import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // version is determined by the implementation dependency of build-logic
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf("-Xjsr305=strict")
        kotlinOptions.jvmTarget = "17"
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
