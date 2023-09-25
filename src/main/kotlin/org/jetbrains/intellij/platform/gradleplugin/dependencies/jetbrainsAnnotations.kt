// Copyright 2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

@file:Suppress("unused")

package org.jetbrains.intellij.platform.gradleplugin.dependencies

import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.create
import org.jetbrains.intellij.platform.gradleplugin.IntelliJPluginConstants.ANNOTATIONS_DEPENDENCY_VERSION
import org.jetbrains.intellij.platform.gradleplugin.IntelliJPluginConstants.Configurations

fun DependencyHandlerScope.jetbrainsAnnotations(
    version: String = ANNOTATIONS_DEPENDENCY_VERSION,
    configurationName: String = Configurations.INTELLIJ_PLATFORM_DEPENDENCIES,
) = create(
    group = "org.jetbrains",
    name = "annotations",
    version = version,
).let { dependency ->
    add(configurationName, dependency)
}
