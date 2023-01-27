@file:Suppress("UnstableApiUsage")

package com.healthmetrix.recontact.buildlogic.conventions

import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.RegisteringDomainObjectDelegateProviderWithTypeAndAction
import org.gradle.kotlin.dsl.invoke
import org.gradle.testing.base.TestSuite
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * Because of the jvm-test-suites purposely not extending the test suites another work around is required sadly.
 * Issues:
 * - https://github.com/gradle/gradle/issues/19497#issuecomment-1007453808
 * - https://github.com/gradle/gradle/issues/19686 (still open)
 *
 * The below code is mainly copied from the second issue. If resolved, please replace this.
 */

fun <T : TestSuite, C : PolymorphicDomainObjectContainer<T>> C.registeringExtended(
    suiteToExtend: JvmTestSuite,
    junitVersion: String,
    action: JvmTestSuite.() -> Unit,
): RegisteringDomainObjectDelegateProviderWithTypeAndAction<out C, JvmTestSuite> =
    RegisteringDomainObjectDelegateProviderWithTypeAndAction.of(
        this,
        JvmTestSuite::class,
        extendFromConfigs(suiteToExtend, junitVersion, action),
    )

private fun extendFromConfigs(
    suiteToExtendFrom: JvmTestSuite,
    junitVersion: String,
    subAction: JvmTestSuite.() -> Unit,
): JvmTestSuite.() -> Unit {
    var extendFromConfigMap: Map<String, Configuration>? = null

    suiteToExtendFrom.targets {
        all {
            val theTestTask = testTask.get()
            val project: Project = theTestTask.project
            extendFromConfigMap = configMapFor(project, theTestTask)
        }
    }

    return {
        // Default junit version of gradle is not the latest
        useJUnitJupiter(junitVersion)

        dependencies {
            implementation(project())
        }

        targets {
            all {
                testTask {
                    shouldRunAfter(suiteToExtendFrom)
                    val extendedConfigMap = configMapFor(this.project, this)
                    extendedConfigMap.forEach { (name, config) ->
                        config.extendsFrom(extendFromConfigMap!![name])
                    }
                }
            }
        }
        this.apply(subAction)
    }
}

private fun configMapFor(project: Project, testTask: Test): Map<String, Configuration> {
    val configMap = mutableMapOf<String, Configuration>()

    project.configurations.forEach { config ->
        if (config.name.startsWith(testTask.name)) {
            configMap[config.name.removePrefix(testTask.name)] = config
        }
    }

    return configMap
}
