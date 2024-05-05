import com.google.gson.Gson
import com.intellij.openapi.Disposable
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

private fun parseDeclaration(declaration: KtNamedDeclaration): Map<String, Any?>? {
    return when (declaration) {
        is KtNamedFunction ->  {
            mapOf(
                "type" to "function",
                "name" to declaration.name,
                "parameters" to declaration.valueParameters.map { mapOf("name" to it.name, "type" to it.typeReference?.text) },
                "returnType" to (declaration.typeReference?.text ?: "Unit"),
                "body" to declaration.text.trimIndent()
            )
        }
        is KtClass -> {
            val allProperties = getConstructorProperties(declaration) + getClassProperties(declaration)
            mapOf(
                "type" to "class",
                "name" to declaration.name,
                "properties" to allProperties,
                "body" to declaration.text.trim()
            )
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

    val declarations = PsiTreeUtil.findChildrenOfType(psiFile, KtNamedDeclaration::class.java)
    val decls = declarations.mapNotNull { parseDeclaration(it) }

    println(Gson().toJson(mapOf("declarations" to decls)))
}
