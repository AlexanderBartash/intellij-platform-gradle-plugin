// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.PLUGIN_GROUP_NAME
import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.VERSION_LATEST
import org.jetbrains.intellij.platform.gradle.asPath
import org.jetbrains.intellij.platform.gradle.utils.LatestVersionResolver
import java.nio.file.Path
import kotlin.io.path.copyTo

/**
 * Resolves and downloads Marketplace ZIP Signer CLI tool used by the [SignPluginTask] task.
 */
@Deprecated(message = "CHECK")
@DisableCachingByDefault(because = "Resolves value from remote source")
abstract class DownloadZipSignerTask : DefaultTask() {

    /**
     * Returns the version of the Marketplace ZIP Signer CLI that will be used.
     *
     * Default value: `LATEST`
     */
    @get:Input
    @get:Optional
    abstract val version: Property<String>

    /**
     * The output of the ZIP Signer CLI tool.
     */
    @get:Input
    @get:Optional
    abstract val cliPath: Property<String>

    /**
     * Local path to the Marketplace ZIP Signer CLI that will be used.
     */
    @get:OutputFile
    abstract val cli: RegularFileProperty

    init {
        group = PLUGIN_GROUP_NAME
        description = "Downloads marketplace-zip-signer library."
    }

    @TaskAction
    fun downloadZipSigner() {
        Path.of(cliPath.get()).copyTo(cli.asPath, true)
    }

    /**
     * Resolves the Marketplace ZIP Signer CLI version.
     * If set to [VERSION_LATEST], there's request to [MARKETPLACE_ZIP_SIGNER_URL] performed for the latest available version.
     *
     * @return Marketplace ZIP Signer CLI version
     */
    internal fun resolveCliVersion(version: String?) = version?.takeIf { it != VERSION_LATEST } ?: resolveLatestVersion()

    /**
     * Resolves Marketplace ZIP Signer CLI download URL.
     *
     * @return Marketplace ZIP Signer CLI download URL
     */
    internal fun resolveCliUrl(version: String?) = resolveCliVersion(version).let {
        RELEASE_DOWNLOAD_URL.replace("%VERSION%", it)
    }

    companion object {
        private const val MARKETPLACE_ZIP_SIGNER_URL = "https://github.com/JetBrains/marketplace-zip-signer"
        private const val RELEASE_DOWNLOAD_URL = "$MARKETPLACE_ZIP_SIGNER_URL/releases/download/%VERSION%/marketplace-zip-signer-cli.jar"

        /**
         * Resolves the latest version available of the Marketplace ZIP Signer CLI using GitHub API.
         *
         * @return latest CLI version
         */
        fun resolveLatestVersion() = LatestVersionResolver.fromGitHub("Marketplace ZIP Signer CLI", MARKETPLACE_ZIP_SIGNER_URL)
    }
}
