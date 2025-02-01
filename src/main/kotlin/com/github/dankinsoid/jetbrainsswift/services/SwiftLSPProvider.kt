import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.LspServerSupportProvider.LspServerStarter
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.*

class SwiftLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerStarter) {
        println("fileOpened")
        if (isSPMProject(file, project)) {
            val descriptor = SwiftLspServerDescriptor(project)
            if (descriptor.isSupportedFile(file)) {
                println("ensureServerStarted")
                serverStarter.ensureServerStarted(descriptor)
            }
        }
    }

    private fun isSPMProject(file: VirtualFile, project: Project): Boolean {
        return true
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val moduleRoot = projectFileIndex.getContentRootForFile(file) ?: return false
        val packageSwiftPath = Paths.get(moduleRoot.path, "Package.swift")
        return VfsUtil.findFileByIoFile(packageSwiftPath.toFile(), true) != null
    }
}

private class SwiftLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Swift") {
    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.extension == "swift" || file.extension == "h" || file.extension == "m" || file.extension == "mm"
    }
    override fun createCommandLine(): GeneralCommandLine {
        println("createCommandLine")
        val sourceKitLSPPath = "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/sourcekit-lsp"

        if (!File(sourceKitLSPPath).exists()) {
            throw Exception("sourcekit-lsp not found")
        }

        println("sourceKitLSPPath: $sourceKitLSPPath")
        return GeneralCommandLine().apply {
            exePath = sourceKitLSPPath // Set the path to the sourcekit-lsp executable
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            withCharset(StandardCharsets.UTF_8)
            addParameter("--stdio") // Add any necessary command line arguments
        }
    }
    private fun findSourceKitLSP(): String? {
        try {
            val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
            val command = if (osName.contains("win")) "where sourcekit-lsp" else "which sourcekit-lsp"

            val process = Runtime.getRuntime().exec(command)
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                return reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}