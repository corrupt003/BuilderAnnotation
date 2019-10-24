package com.corrupt003.android.builder_processor;

import com.corrupt003.android.builder_annotation.Builder;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuilderProcessor extends AbstractProcessor {
    private Messager mMessager;
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessager = processingEnvironment.getMessager();
        mFiler = processingEnvironment.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add(Builder.class.getCanonicalName());
        return set;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // We only care about `Builder` annotation.
        Set<? extends Element> annotatedElements =
                roundEnvironment.getElementsAnnotatedWith(Builder.class);

        for (Element element : annotatedElements) {
            String packageName = getPackageName(element);
            String typeName = getTypeName(element);
            List<VariableElement> vars = getNonPrivateFields(element);

            String builderName = String.format("%sBuilder", typeName);
            ClassName builderType = ClassName.get(packageName, builderName);

            TypeSpec builder = createBuilder(vars, builderType, element, builderName);

            // Write java source file.
            JavaFile file =
                    JavaFile.builder(packageName, builder)
                            .build();

            try {
                file.writeTo(mFiler);
            } catch (IOException e) {
                mMessager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to write file: " + e.toString()
                );
                return false;
            }
        }

        return true;
    }

    private String getPackageName(Element typeElement) {
        return this.processingEnv
                .getElementUtils()
                .getPackageOf(typeElement)
                .getQualifiedName()
                .toString();
    }

    private String getTypeName(Element typeElement) {
        return typeElement.getSimpleName().toString();
    }

    private List<VariableElement> getNonPrivateFields(Element typeElement) {
        List<VariableElement> elements = new ArrayList<>();
        for (Element element : typeElement.getEnclosedElements()) {
            if (element instanceof VariableElement) {
                Set<Modifier> modifiers = element.getModifiers();
                if (modifiers.isEmpty() || !modifiers.contains(Modifier.PRIVATE)) {
                    elements.add((VariableElement) element);
                }
            }
        }

        return elements;
    }

    private TypeSpec createBuilder(
            List<VariableElement> vars,
            ClassName builderType,
            Element element,
            String builderName
    ) {
        // Create private fields and public setters
        List<FieldSpec> fields = new ArrayList<>(vars.size());
        List<MethodSpec> setters = new ArrayList<>(vars.size());

        for (VariableElement var : vars) {
            TypeName typeName = TypeName.get(var.asType());
            String name = var.getSimpleName().toString();

            // Create the private field.
            fields.add(FieldSpec.builder(typeName, name, Modifier.PRIVATE).build());

            // Create the public setter.
            setters.add(MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(typeName, name)
                    .returns(builderType)
                    .addStatement("this.$N = $N", name, name)
                    .addStatement("return this")
                    .build());
        }

        String retValName = "temp";
        // Create the build method.
        TypeName targetType = TypeName.get(element.asType());
        MethodSpec.Builder methodBuilder =
                MethodSpec.methodBuilder("build")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(targetType)
                        .addStatement("$1T $2N = new $1T()", targetType, retValName);

        for (FieldSpec field : fields) {
            methodBuilder.addStatement("$1N.$2N = this.$2N", retValName, field.name);
        }

        methodBuilder.addStatement("return $N", retValName);
        MethodSpec buildMethod = methodBuilder.build();
        // Create the builder.
        return TypeSpec.classBuilder(builderName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(fields)
                .addMethods(setters)
                .addMethod(buildMethod)
                .build();
    }
}
