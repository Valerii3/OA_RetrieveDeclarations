import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.io.File


private fun setupKotlinEnvironment(): Project {
    val configuration = CompilerConfiguration()
    val environment = KotlinCoreEnvironment.createForProduction(
        parentDisposable = { },
        configuration = configuration,
        configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
    return environment.project
}

private fun getClassProperties(declaration: KtClass) = declaration.getProperties().map { property ->
     mapOf("name" to property.name, "type" to property.typeReference?.text)
 }

private fun getConstructorProperties(declaration: KtClass) = declaration.primaryConstructor?.valueParameters?.map { mapOf("name" to it.name, "type" to it.typeReference?.text) } ?: emptyList()

private fun getBody(declaration: KtNamedDeclaration) = declaration.text.replace(Regex("\\s+"), " ").trim()

private fun mapWithNestedDeclarations(baseMap: Map<String, Any?>, nestedDeclarations: List<Map<String, Any?>>): Map<String, Any?> {
    if (nestedDeclarations.isEmpty()) return baseMap
    return baseMap + mapOf("declarations" to nestedDeclarations)
}

private fun parseDeclaration(declaration: KtNamedDeclaration): Map<String, Any?>? {
    return when (declaration) {
        is KtNamedFunction ->  {
            val nestedDeclarations = PsiTreeUtil.findChildrenOfType(declaration, KtNamedDeclaration::class.java)
                .mapNotNull { parseDeclaration(it) }

            val baseMap = mapOf(
                "type" to "function",
                "name" to declaration.name,
                "parameters" to declaration.valueParameters.map { mapOf("name" to it.name, "type" to it.typeReference?.text) },
                "returnType" to (declaration.typeReference?.text ?: "Unit"),
                "body" to getBody(declaration)
            )
            mapWithNestedDeclarations(baseMap, nestedDeclarations)

        }
        is KtClass -> {
            val allProperties = getConstructorProperties(declaration) + getClassProperties(declaration)
            val nestedDeclarations = PsiTreeUtil.findChildrenOfType(declaration, KtNamedDeclaration::class.java)
                .mapNotNull { parseDeclaration(it) }

            val baseMap = mapOf(
                "type" to "class",
                "name" to declaration.name,
                "properties" to allProperties,
                "body" to getBody(declaration)
            )
            mapWithNestedDeclarations(baseMap, nestedDeclarations)
        }
        else -> null
    }
}

fun main(args: Array<String>) {
    val env = setupKotlinEnvironment()
    val content = File(args[0]).readText().trimIndent()

    val psiFile = PsiFileFactory.getInstance(env).createFileFromText(
        "example.kt",
        KotlinFileType.INSTANCE,
        content
    )

    val rootDeclarations = PsiTreeUtil.findChildrenOfType(psiFile, KtNamedDeclaration::class.java)
        .filter { it.parent == psiFile } // top-level declarations
    val decls = rootDeclarations.mapNotNull { parseDeclaration(it) }

    val gson = GsonBuilder().disableHtmlEscaping().create() // escaping && and ==
    println(gson.toJson(mapOf("declarations" to decls)))
}
