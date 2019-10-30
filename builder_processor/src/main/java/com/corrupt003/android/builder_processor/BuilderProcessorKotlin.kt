package com.corrupt003.android.builder_processor

import com.corrupt003.android.builder_annotation.BuilderKotlin
import com.squareup.kotlinpoet.*
import java.io.IOException
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

@SupportedAnnotationTypes("com.corrupt003.android.builder_annotation.BuilderKotlin")
// @SupportedOptions(BuilderProcessorKotlin.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class BuilderProcessorKotlin : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(
        set: Set<TypeElement>,
        roundEnvironment: RoundEnvironment
    ): Boolean {
        val annotatedElements = roundEnvironment
            .getElementsAnnotatedWith(BuilderKotlin::class.java)

        if (annotatedElements.isEmpty()) {
            return false
        }

        /*
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: run {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can't find the target directory for generated Kotlin files."
            )
            return false
        }
        */

        for (element in annotatedElements) {
            val packageName = getPackageName(element)
            val typeName = element.simpleName.toString()
            val vars = getAllProperties(element)

            if (vars.isEmpty()) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Can't find any non private properties."
                )
                return false
            }

            val builderName = String.format("%sBuilder", typeName)
            val builderType = ClassName(packageName, builderName)

            val builder = createBuilder(vars, builderType, element, builderName)
            val file = FileSpec.builder(packageName, builderName)
                .addType(builder)
                .build()

            try {
                // file.writeTo(File(kaptKotlinGeneratedDir))
                file.writeTo(this.processingEnv.filer)
            } catch (e: IOException) {
                this.processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to write file: $e"
                )
            }
        }
        return false
    }

    private fun getPackageName(typeElement: Element): String {
        return this.processingEnv
            .elementUtils
            .getPackageOf(typeElement)
            .qualifiedName
            .toString()
    }

    private fun getAllProperties(typeElement: Element): List<VariableElement> {
        val elements = ArrayList<VariableElement>()

        for (element in typeElement.enclosedElements) {
            if (element is VariableElement && element.kind == ElementKind.FIELD) {
                elements.add(element)
            }
        }

        return elements
    }

    private fun createBuilder(
        vars: List<VariableElement>,
        builderType: ClassName,
        element: Element,
        builderName: String
    ): TypeSpec {
        // Create private fields and public setters.
        val properties = ArrayList<PropertySpec>(vars.size)
        val setters = ArrayList<FunSpec>(vars.size)

        for (variable in vars) {
            // Create the private property.
            val typeName: TypeName = variable.asType().asTypeName()
            val name = variable.simpleName.toString()

            when (typeName) {
                String::class.java.asTypeName() -> {
                    properties.add(PropertySpec.builder(name, String::class.asTypeName())
                        .mutable(true)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("\"\"")
                        .build()
                    )

                    // Create the public setter function.
                    setters.add(FunSpec.builder(name)
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter(name, String::class.asTypeName())
                        .returns(builderType)
                        .addStatement("this.%N = %N", name, name)
                        .addStatement("return this")
                        .build())
                }
                Int::class.asTypeName() -> {
                    properties.add(PropertySpec.builder(name, typeName)
                        .mutable(true)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("0")
                        .build()
                    )

                    // Create the public setter function.
                    setters.add(FunSpec.builder(name)
                        .addModifiers(KModifier.PUBLIC)
                        .addParameter(name, typeName)
                        .returns(builderType)
                        .addStatement("this.%N = %N", name, name)
                        .addStatement("return this")
                        .build())
                }
                else -> {
                    this.processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Unhandled type name: $typeName"
                    )
                    throw RuntimeException("Unhandled type name: $typeName")
                }
            }
        }

        val retValName = "temp"
        val targetType = element.asType().asTypeName()
        val funBuilder =
            FunSpec.builder("build")
                .addModifiers(KModifier.PUBLIC)
                .returns(targetType)
                .addStatement("val %2N = %1T()", targetType, retValName)

        for (property in properties) {
            funBuilder.addStatement("%1N.%2N = this.%2N", retValName, property.name)
        }

        funBuilder.addStatement("return %N", retValName)
        val funBuild = funBuilder.build()

        return TypeSpec.classBuilder(builderName)
            .addModifiers(KModifier.PUBLIC)
            .addProperties(properties)
            .addFunctions(setters)
            .addFunction(funBuild)
            .build()
    }
}