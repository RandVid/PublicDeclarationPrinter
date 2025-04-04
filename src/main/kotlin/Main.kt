// https://github.com/RandVid/PublicDeclarationPrinter
/**
 * I thought that the sample usage is too difficult for a person to read
 * So I have decided to add several features such as
 * printing file names and their local path
 */

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.lexer.KtTokens
import java.io.File


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: program <source-directory>")
        return
    }

    val sourceDir = File(args[0])
    if (!sourceDir.exists() || !sourceDir.isDirectory) {
        println("Error: ${args[0]} is not a valid directory")
        return
    }

    val configuration = CompilerConfiguration()
    val environment = KotlinCoreEnvironment.createForProduction(
        {},
        configuration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
    proceedKotlinFiles(sourceDir, environment)
}

/**
 * processes all Kotlin files in the given directory,
 * printing their public declarations
 */
private fun proceedKotlinFiles(
    directory: File,
    environment: KotlinCoreEnvironment
) {
    val psiFactory = KtPsiFactory(environment.project)
    directory.walk().filter { it.isFile && it.extension == "kt" }.forEach { file ->
        try {
            val ktFile = psiFactory.createFile(file.readText())
            println("\n\n====== ${file.relativeTo(directory).path} ======")
            printDeclarations(ktFile.declarations)
        } catch (e: Exception) {
            println("Error processing file ${file.path}: ${e.message}")
        }
    }
}


/**
 * isPublicDeclaration
 * Accepts the declaration as a parameter, and return whether it is public
 */
private fun isPublicDeclaration(declaration: KtDeclaration): Boolean {
    if (declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
        declaration.hasModifier(KtTokens.INTERNAL_KEYWORD) ||
        declaration.hasModifier(KtTokens.PROTECTED_KEYWORD))
        return false


    val parent = declaration.getParentOfType<KtDeclaration>(true)
    return parent == null || isPublicDeclaration(parent)
}

/**
 * Prints all public declarations in the given list, handling different
 * declaration types (functions, classes, properties, etc.) with proper formatting.
 */
private fun printDeclarations(declarations: List<KtDeclaration>, indent: String = "") {
    for (declaration in declarations) {
        if (!isPublicDeclaration(declaration)) continue
        when (declaration) {
            is KtNamedFunction -> {
                val name = declaration.name ?: continue
                val receiver = declaration.receiverTypeReference?.text?.plus(".") ?: ""
                val typeParams = if (declaration.typeParameters.isNotEmpty()) {
                    declaration.typeParameters.joinToString(", ", "<", ">") { param ->
                        param.name + (param.extendsBound?.text?.let { " : $it" } ?: "")
                    }
                } else ""
                val parameters = declaration.valueParameters
                    .filter { it.name != null } // filter the anonymous parameters
                    .joinToString(", ") { param ->
                        "${param.name}: ${param.typeReference?.text ?: "Any"}"
                    }
                println("${indent}fun ${receiver}${name}${typeParams}($parameters)")
            }

            is KtClass -> {
                val name = declaration.name ?: continue
                val typeParams = if (declaration.typeParameters.isNotEmpty()) {
                    declaration.typeParameters.joinToString(", ", "<", ">") { it.name ?: "" }
                } else ""
                val classType = when {
                    declaration.isInterface() -> "interface"
                    declaration.isEnum() -> "enum class"
                    declaration.isData() -> "data class"
                    declaration.hasModifier(KtTokens.ABSTRACT_KEYWORD) -> "abstract class"
                    else -> "class"
                }
                if (declaration.isInterface()) println("${indent}interface $name$typeParams {")
                else println("${indent}$classType $name$typeParams {")
                declaration.body?.declarations?.let { printDeclarations(it, "$indent    ") }
                println("${indent}}")
            }

            is KtProperty -> {
                val name = declaration.name ?: continue
                val type = if (declaration.typeReference != null) ": ${declaration.typeReference?.text}" else ""
                val mutable = if (declaration.isVar) "var" else "val"
                println("${indent}$mutable ${name}$type")
            }

            is KtObjectDeclaration -> {
                val name = declaration.name ?: continue
                if (name == "Companion") println("${indent}companion object {")
                else println("${indent}object $name {")
                declaration.body?.declarations?.let { printDeclarations(it, "$indent    ") }
                println("${indent}}")
            }

            is KtConstructor<*> -> {
                val containingClass = declaration.getParentOfType<KtClass>(true)
                if (containingClass != null && !isPublicDeclaration(containingClass)) continue

                val parameters = declaration.valueParameters.joinToString(", ") { param ->
                    val defaultValue = param.defaultValue?.let { " = ${it.text}" } ?: ""
                    "${param.name}: ${param.typeReference?.text ?: "Any"}$defaultValue"
                }
                println("${indent}constructor($parameters)")
            }
        }
    }
}