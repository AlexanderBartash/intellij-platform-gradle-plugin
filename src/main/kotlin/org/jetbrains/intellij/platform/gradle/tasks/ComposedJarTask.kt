// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.intellij.platform.gradle.Constants.Components
import org.jetbrains.intellij.platform.gradle.Constants.Configurations
import org.jetbrains.intellij.platform.gradle.Constants.Plugin
import org.jetbrains.intellij.platform.gradle.Constants.Tasks
import org.jetbrains.intellij.platform.gradle.tasks.compaion.JarCompanion
import org.jetbrains.intellij.platform.gradle.utils.extensionProvider

@CacheableTask
abstract class ComposedJarTask : Jar() {

    init {
        group = Plugin.GROUP_NAME
        description = "Prepares a Jar archive with all the modules of the plugin."
    }

    companion object : Registrable {
        override fun register(project: Project) =
            project.registerTask<ComposedJarTask>(Tasks.COMPOSED_JAR) {
                val softwareComponentFactory = project.serviceOf<SoftwareComponentFactory>()
                val jarTaskProvider = project.tasks.named<Jar>(Tasks.External.JAR)
                val instrumentedJarTaskProvider = project.tasks.named<Jar>(Tasks.INSTRUMENTED_JAR)
                val intellijPlatformPluginModuleConfiguration = project.configurations[Configurations.INTELLIJ_PLATFORM_PLUGIN_MODULE]
                val intellijPlatformComposedJarConfiguration = project.configurations[Configurations.INTELLIJ_PLATFORM_COMPOSED_JAR]

                val sourceTaskProvider = project.extensionProvider.flatMap {
                    it.instrumentCode.flatMap { value ->
                        when (value) {
                            true -> instrumentedJarTaskProvider
                            false -> jarTaskProvider
                        }
                    }
                }

                from(project.zipTree(sourceTaskProvider.flatMap { it.archiveFile }))
                from(project.provider {
                    intellijPlatformPluginModuleConfiguration.map {
                        project.zipTree(it)
                    }
                })

                dependsOn(sourceTaskProvider)
                dependsOn(intellijPlatformPluginModuleConfiguration)

                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                JarCompanion.applyPluginManifest(this)

                project.artifacts.add(intellijPlatformComposedJarConfiguration.name, this)

                softwareComponentFactory.adhoc(Components.INTELLIJ_PLATFORM).apply {
                    project.components.add(this)
                    addVariantsFromConfiguration(intellijPlatformComposedJarConfiguration) {
                        mapToMavenScope("runtime")
                    }
                }
            }
    }
}
