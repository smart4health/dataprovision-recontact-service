plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(mainLibs.kotlin.gradle.plugin)

    implementation(mainLibs.lazysodium)
}
