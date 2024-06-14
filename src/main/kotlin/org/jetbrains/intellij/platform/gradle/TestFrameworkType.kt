// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle

import org.jetbrains.intellij.platform.gradle.models.Coordinates

/**
 * Definition of Test Framework types available for writing tests for IntelliJ Platform plugins.
 *
 * @param coordinates Maven coordinates of test framework artifact.
 */
sealed class TestFrameworkType(vararg val coordinates: Coordinates) {
    object Platform : TestFrameworkType(Coordinates("com.jetbrains.intellij.platform", "test-framework"))
    object JUnit5 : TestFrameworkType(Coordinates("com.jetbrains.intellij.platform", "test-framework-junit5"))
    object Bundled : TestFrameworkType(Coordinates("bundled", "lib/testFramework.jar"))
    object Metrics : TestFrameworkType(
        Coordinates("com.jetbrains.intellij.tools", "ide-metrics-benchmark"),
        Coordinates("com.jetbrains.intellij.tools", "ide-metrics-collector"),
        Coordinates("com.jetbrains.intellij.tools", "ide-util-common"),
    )

    object Plugin {
        object Go : TestFrameworkType(Coordinates("com.jetbrains.intellij.go", "go-test-framework"))
        object Ruby : TestFrameworkType(Coordinates("com.jetbrains.intellij.idea", "ruby-test-framework"))
        object Java : TestFrameworkType(Coordinates("com.jetbrains.intellij.java", "java-test-framework"))
        object JavaScript : TestFrameworkType(Coordinates("com.jetbrains.intellij.javascript", "javascript-test-framework"))
        object Maven : TestFrameworkType(Coordinates("com.jetbrains.intellij.maven", "maven-test-framework"))
        object ReSharper : TestFrameworkType(Coordinates("com.jetbrains.intellij.resharper", "resharper-test-framework"))
    }
}
