import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

// Path to the integration tests single project, like:
// /Users/hsz/Projects/JetBrains/gradle-intellij-plugin/integration-tests/plugin-xml-patching/
val Path.projectDirectory
    get() = toAbsolutePath().parent

// Path to the integration tests root directory, like:
// /Users/hsz/Projects/JetBrains/gradle-intellij-plugin/integration-tests/
val Path.testsRootDirectory
    get() = projectDirectory.parent

// Path to the Gradle IntelliJ Plugin root directory, like:
// /Users/hsz/Projects/JetBrains/gradle-intellij-plugin/
val Path.rootDirectory
    get() = testsRootDirectory.parent

// Path to the build directory of the integration tests single project, like:
// /Users/hsz/Projects/JetBrains/gradle-intellij-plugin/integration-tests/plugin-xml-patching/build/
val Path.buildDirectory
    get() = projectDirectory.resolve("build").exitIf(Files::notExists) { "build directory does not exist: ${toAbsolutePath()}" }

// Path to the Gradle Wrapper – uses first argument provided to the script or falls back to the project instance, like:
// /Users/hsz/Projects/JetBrains/gradle-intellij-plugin/gradlew
val Path.gradleWrapper
    get() = args.firstOrNull() ?: rootDirectory.resolve("gradlew")

// Path to the patched `plugin.xml` file located within the build directory of the integration tests single project, like:
// /Users/hsz/Projects/JetBrains/gradle-intellij-plugin/integration-tests/plugin-xml-patching/build/patchedPluginXmlFiles/plugin.xml
val Path.patchedPluginXml
    get() = buildDirectory
        .resolve("patchedPluginXmlFiles/plugin.xml")
        .exitIf(Files::notExists) { "plugin.xml file does not exist: ${toAbsolutePath()}" }
        .let(Files::readString)

// Runs the given Gradle task(s) within the current integration test.
// Provides logs to STDOUT and as a returned value for the further assertions.
fun Path.runGradleTask(vararg tasks: String) =
    ProcessBuilder()
        .command(gradleWrapper.toString(), *tasks, "--info")
        .directory(testsRootDirectory.toFile())
        .start()
        .run {
            val stdoutBuffer = ByteArrayOutputStream()
            val stderrBuffer = ByteArrayOutputStream()

            inputStream.copyTo(TeeOutputStream(stdoutBuffer, System.out))
            errorStream.copyTo(TeeOutputStream(stderrBuffer, System.err))

            stdoutBuffer.toString() + stderrBuffer.toString()
        }

fun <T> T.exitIf(block: T.() -> Boolean, message: T.() -> String = { "" }): T {
    if (block()) {
        println(message())
        System.exit(-1)
    }
    return this
}

infix fun String.containsText(string: String) {
    assert(contains(string))
}

infix fun String.matchesRegex(regex: String) {
    matchesRegex(regex.toRegex())
}

infix fun String.matchesRegex(regex: Regex) {
    assert(regex.containsMatchIn(this))
}

infix fun Path.containsFile(path: String) {
    assert(resolve(path).let(Files::exists))
}

infix fun Path.containsFileInArchive(path: String) {
    val fs = FileSystems.newFileSystem(this, null as ClassLoader?)
    assert(fs.getPath(path).let(Files::exists))
}

infix fun Path.readEntry(path: String) = ZipFile(toFile()).use { zip ->
    val entry = zip.getEntry(path)
    zip.getInputStream(entry).bufferedReader().use { it.readText() }
}

class TeeOutputStream(vararg targets: OutputStream) : OutputStream() {

    private val targets = targets.toList()

    override fun write(b: Int) = targets.forEach { it.write(b) }

    override fun flush() = targets.forEach(OutputStream::flush)

    override fun close() = targets.forEach(OutputStream::close)
}
