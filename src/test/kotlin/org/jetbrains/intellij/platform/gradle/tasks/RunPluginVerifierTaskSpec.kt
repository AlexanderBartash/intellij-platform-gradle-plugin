// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.tasks

import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.Tasks
import org.jetbrains.intellij.platform.gradle.IntelliJPluginSpecBase
import org.jetbrains.intellij.platform.gradle.utils.LatestVersionResolver
import java.util.*
import kotlin.io.path.*
import kotlin.test.*

class RunPluginVerifierTaskSpec : IntelliJPluginSpecBase() {

    @Test
    fun `warn about no IDE picked for verification`() {
        writePluginVerifierDependency()

        buildAndFail(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("No IDE selected for verification with the IntelliJ Plugin Verifier", output)
        }
    }

    @Test
    fun `run plugin verifier in specified version`() {
        writePluginXmlFile()
        writePluginVerifierDependency("1.307")
        writePluginVerifierIde()

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Starting the IntelliJ Plugin Verifier 1.307", output)
        }
    }

    @Test
    fun `run plugin verifier fails on old version lower than 1_255`() {
        writePluginXmlFile()
        writePluginVerifierDependency("1.254")
        writePluginVerifierIde()

        build(
            gradleVersion = gradleVersion,
            fail = true,
            assertValidConfigurationCache = false,
            Tasks.RUN_PLUGIN_VERIFIER,
        ) {
            assertContains("Could not find org.jetbrains.intellij.plugins:verifier-cli:1.254", output)
        }
    }

    @Test
    fun `run plugin verifier in the latest version`() {
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            val version = LatestVersionResolver.pluginVerifier()
            assertContains("Starting the IntelliJ Plugin Verifier $version", output)
        }
    }

    @Test
    fun `test plugin against two IDEs`() {
        writeJavaFile()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()
        writePluginVerifierIde("PS", "2022.3")

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Plugin projectName:1.0.0 against IC-223.8836.41: Compatible", output)
            assertContains("Plugin projectName:1.0.0 against PS-223.7571.212: Compatible", output)
        }
    }

    @Test
    fun `test plugin against Android Studio`() {
        writeJavaFile()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde("AI", "2022.3.1.18")

        buildFile.kotlin(
            """
            repositories {
                intellijPlatform {
                    binaryReleasesAndroidStudio()
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Plugin projectName:1.0.0 against AI-223.8836.35.2231.10406996: Compatible", output)
        }
    }

    @Test
    fun `set verification reports directory`() {
        writeJavaFile()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    verificationReportsDirectory = project.layout.buildDirectory.dir("foo")
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            val directory = buildDirectory.resolve("foo")
            assertContains("Verification reports directory: ${directory.toRealPath()}", output)
        }
    }

    @Test
    fun `set verification reports output formats`() {
        writeJavaFile()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    verificationReportsDirectory = project.layout.buildDirectory.dir("foo")
                    verificationReportsFormats = listOf(VerificationReportsFormats.MARKDOWN, VerificationReportsFormats.PLAIN)
                }
            }
            """.trimIndent()
        )

        println("buildFile = ${buildFile}")

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            val reportsDirectory = buildDirectory.resolve("foo")
            assertContains("Verification reports directory: ${reportsDirectory.toRealPath()}", output)

            val ideVersionDir = reportsDirectory.listDirectoryEntries().firstOrNull()
            assertNotNull(ideVersionDir, "Verification reports directory not found")

            val markdownReportFiles = ideVersionDir.listDirectoryEntries("*.md")
            assertEquals(1, markdownReportFiles.size)
        }
    }

    @Test
    fun `set verification reports with empty set of output formats`() {
        writeJavaFile()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    verificationReportsFormats.empty()
                    verificationReportsDirectory = project.layout.buildDirectory.dir("foo")
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            val reportsDirectory = buildDirectory.resolve("foo")
            assertContains("Verification reports directory: ${reportsDirectory.toRealPath()}", output)

            val ideVersionDir = reportsDirectory.listDirectoryEntries().firstOrNull()
            assertNotNull(ideVersionDir, "Verification reports directory not found")

            val reportFiles = ideVersionDir.listDirectoryEntries("*.{md,html}")
            assertTrue(reportFiles.isEmpty())
        }
    }

    @Test
    fun `set verification reports with default settings`() {
        writeJavaFile()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    verificationReportsDirectory = project.layout.buildDirectory.dir("foo")
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            val reportsDirectory = buildDirectory.resolve("foo")
            assertContains("Verification reports directory: ${reportsDirectory.toRealPath()}", output)

            val ideVersionDir = reportsDirectory.listDirectoryEntries().firstOrNull()
            assertNotNull(ideVersionDir, "Verification reports directory not found")

            val reportFiles = ideVersionDir.listDirectoryEntries("*.{md,html}")
            assertTrue(reportFiles.isNotEmpty())
        }
    }

    @Test
    fun `set ignored problems file`() {
        writeJavaFileWithPluginProblems(classNameSuffix = UUID.randomUUID().toString().replace("-", ""))
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        val lines = listOf("projectName:1.0.0:Reference to a missing property.*")
        val ignoredProblems = createTempFile("ignored-problems", ".txt").writeLines(lines)

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    ignoredProblemsFile = file("${ignoredProblems.invariantSeparatorsPathString}")
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Compatible. 1 usage of scheduled for removal API and 1 usage of deprecated API. 1 usage of internal API", output)
            assertNotContains("Reference to a missing property", output)
        }
    }

    @Test
    fun `fail on verifyPlugin task`() {
        writeJavaFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        pluginXml.deleteIfExists()

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("The plugin descriptor 'plugin.xml' is not found.", output)
        }
    }

    @Test
    fun `fail on Deprecated API usages`() {
        writeJavaFileWithDeprecation()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    failureLevel = listOf(FailureLevel.DEPRECATED_API_USAGES)
                }
            }
            """.trimIndent()
        )

        buildAndFail(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Deprecated API usages", output)
            assertContains("org.gradle.api.GradleException: DEPRECATED_API_USAGES", output)
        }
    }

    @Test
    fun `pass on Deprecated API usages`() {
        writeJavaFileWithDeprecation()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Deprecated API usages", output)
            assertNotContains("org.gradle.api.GradleException: DEPRECATED_API_USAGES", output)
        }
    }

    @Test
    fun `fail on incorrect ide version`() {
        writeJavaFile()
        writePluginXmlFile()
        writePluginVerifierDependency()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    ides {
                        ide("foo")
                    }
                }
            }
            """.trimIndent()
        )

        buildAndFail(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Could not find idea:ideaIC:foo.", output)
        }
    }

    @Test
    fun `pass on recommended ides`() {
        writeJavaFile()
        writePluginXmlFile()
        writePluginVerifierDependency()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    ides {
                        recommended()
                    }
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Reading IDE", output)
            assertContains("pluginVerifier/ides/IC-2022.3.3", output)
        }
    }

    @Test
    fun `fail on any failureLevel`() {
        writeJavaFileWithDeprecation()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """            
            intellijPlatform {
                pluginVerifier {
                    failureLevel = FailureLevel.ALL
                }
            }
            """.trimIndent()
        )

        buildAndFail(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Deprecated API usages", output)
            assertContains("org.gradle.api.GradleException: DEPRECATED_API_USAGES", output)
        }
    }

    @Test
    fun `pass on any failureLevel`() {
        writeJavaFileWithDeprecation()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """
            tasks {
                runPluginVerifier {
                    failureLevel = RunPluginVerifierTask.FailureLevel.NONE
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertContains("Deprecated API usages", output)
            assertNotContains("org.gradle.api.GradleException: DEPRECATED_API_USAGES", output)
        }
    }

    @Test
    fun `run plugin verifier in offline mode`() {
        writePluginXmlFile()
        writePluginVerifierIde()
        build(Tasks.BUILD_PLUGIN)

        writePluginVerifierDependency()
        writePluginVerifierIde(version = "2022.3.1")

        buildAndFail(Tasks.RUN_PLUGIN_VERIFIER, "--offline") {
            assertContains("Could not resolve idea:ideaIC:2022.3.1", output)
            assertContains("No cached version of idea:ideaIC:2022.3.1 available for offline mode.", output)
        }
    }

    @Test
    @Ignore
    fun `pass on CLI arguments passed as free args`() {
        writeJavaFileWithDeprecation()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    verificationReportsDirectory = project.layout.buildDirectory.dir("foo")
                    freeArgs = listOf("-verification-reports-formats", "plain") 
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            val reportsDirectory = buildDirectory.resolve("foo")
            assertContains("Verification reports directory: ${reportsDirectory.toRealPath()}", output)

            val ideVersionDir = reportsDirectory.listDirectoryEntries().firstOrNull()
            assertNotNull(ideVersionDir, "Verification reports directory not found")

            val reportFiles = ideVersionDir.listDirectoryEntries("*.{md,html}")
            assertTrue(reportFiles.isEmpty())
        }
    }

    @Test
    fun `pass on CLI arguments the internal API usage mode as a free arg`() {
        writeJavaFileWithInternalApiUsage()
        writePluginXmlFile()
        writePluginVerifierDependency()
        writePluginVerifierIde()

        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    freeArgs = listOf("-suppress-internal-api-usages", "jetbrains-plugins") 
                }
            }
            """.trimIndent()
        )

        build(Tasks.RUN_PLUGIN_VERIFIER) {
            assertNotContains("Internal API usages (2):", output)
        }
    }

    private fun writePluginVerifierDependency(version: String? = null) {
        buildFile.kotlin(
            """
            repositories {
                intellijPlatform {
                    binaryReleases()
                }
            }
            dependencies {
                intellijPlatform {
                    pluginVerifier(${version?.let { "\"$it\"" }.orEmpty()})
                }
            }
            """.trimIndent()
        )
    }

    private fun writePluginVerifierIde(type: String = intellijType, version: String = intellijVersion) {
        buildFile.kotlin(
            """
            intellijPlatform {
                pluginVerifier {
                    ides {
                        ide("$type", "$version")
                    }
                }
            }
            """.trimIndent()
        )
    }

    private fun writeJavaFileWithDeprecation() {
        dir.resolve("src/main/java/App.java").java(
            """  
            import java.lang.String;
            import org.jetbrains.annotations.NotNull;
            import com.intellij.openapi.util.text.StringUtil;
            
            class App {
            
                public static void main(@NotNull String[] strings) {
                    StringUtil.escapeXml("<foo>");
                }
            }
            """.trimIndent()
        )
    }

    private fun writeJavaFileWithPluginProblems(classNameSuffix: String) {
        @Suppress("UnresolvedPropertyKey", "ResultOfMethodCallIgnored")
        dir.resolve("src/main/java/App$classNameSuffix.java").java(
            """  
            class App$classNameSuffix {
                public static String message(@org.jetbrains.annotations.PropertyKey(resourceBundle = "messages.ActionsBundle") String key, Object... params) {
                    return null;
                }
            
                public static void main(String[] args) {
                    App$classNameSuffix.message("somemessage", "someparam1");
                
                    System.out.println(com.intellij.openapi.project.ProjectCoreUtil.theProject);
                    
                    com.intellij.openapi.project.ProjectCoreUtil util = new com.intellij.openapi.project.ProjectCoreUtil();
                    System.out.println(util.theProject);
                    
                    System.out.println(com.intellij.openapi.project.Project.DIRECTORY_STORE_FOLDER);
                    com.intellij.openapi.components.ServiceManager.getService(String.class);
                }
            }
            """.trimIndent()
        )
    }

    private fun writeJavaFileWithInternalApiUsage() {
        dir.resolve("src/main/java/App.java").java(
            """  
            class App {
                public static void main(String[] args) {
                    new com.intellij.DynamicBundle.LanguageBundleEP();
                }
            }
            """.trimIndent()
        )
    }

    private fun writePluginXmlFile() {
        pluginXml.xml(
            """
            <idea-plugin>
                <name>MyName</name>
                <description>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</description>
                <vendor>JetBrains</vendor>
                <depends>com.intellij.modules.platform</depends>
            </idea-plugin>
            """.trimIndent()
        )
    }
}