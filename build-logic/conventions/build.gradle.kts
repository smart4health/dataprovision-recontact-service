plugins {
    `kotlin-dsl`
}

// differing target compatibility warnings are caused by build-logic, and could be fixed...
// alas, 17 is too new for the kotlin-dsl.  This should be fixed in newer gradle versions.
// kotlinDslPluginOptions { jvmTarget.set(JavaVersion.VERSION_17.toString()) }

dependencies {
    implementation(mainLibs.kotlin.gradle.plugin)
}
