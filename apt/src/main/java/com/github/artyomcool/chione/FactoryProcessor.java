/*
 * The MIT License
 *
 * Copyright (c) 2018 Artyom Drozdov (https://github.com/artyomcool)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.artyomcool.chione;

import com.squareup.javapoet.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static javax.lang.model.element.Modifier.*;

class FactoryProcessor extends Processor<Factory> {

    FactoryProcessor() {
        super(Factory.class);
    }

    @Override
    protected JavaFile generate(TypeElement element) {
        if (!element.getKind().isInterface()) {
            throw new IllegalArgumentException("@Factory supports only interfaces");
        }

        TypeSpec.Builder moduleBuilder = TypeSpec.classBuilder(element.getSimpleName() + "Module")
                .addModifiers(PUBLIC)
                .addSuperinterface(moduleType(element));

        moduleBuilder.addField(DataFile.class, "file", PRIVATE, FINAL);

        moduleBuilder.addField(Chione.class, "chione", PRIVATE, FINAL);

        moduleBuilder.addField(chioneWrapper(element), "chioneWrapper", PRIVATE, FINAL);

        moduleBuilder.addField(factoryClass(element), "factory", PRIVATE, FINAL);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)
                .addParameter(DataFile.class, "file")
                .addCode(
                        CodeBlock.builder()
                                .addStatement("this.file = file")
                                .addStatement("this.chione = new $T(createSerializers())", Chione.class)
                                .addStatement(
                                        "this.chioneWrapper = new $T<>(chione, file)",
                                        SimpleChioneWrapper.class
                                )
                                .addStatement("this.factory = $L", iceFactory(element))
                                .build()
                )
                .build();

        moduleBuilder.addMethod(constructor);

        MethodSpec chione = MethodSpec.methodBuilder("chione")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(chioneWrapper(element))
                .addCode("return chioneWrapper;\n")
                .build();

        moduleBuilder.addMethod(chione);

        MethodSpec factory = MethodSpec.methodBuilder("factory")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(factoryClass(element))
                .addCode("return factory;\n")
                .build();

        moduleBuilder.addMethod(factory);

        MethodSpec createSerializers = MethodSpec.methodBuilder("createSerializers")
                .addModifiers(PRIVATE, STATIC)
                .returns(serializersMap())
                .addCode(createSerializers(element))
                .build();

        moduleBuilder.addMethod(createSerializers);

        return JavaFile.builder(factoryClass(element).packageName(), moduleBuilder.build())
                .skipJavaLangImports(true)
                .build();
    }

    private CodeBlock createSerializers(TypeElement element) {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.addStatement("$T m = new $T<>()", serializersMap(), HashMap.class);
        VoidVisitor visitor = new VoidVisitor() {
            @Override
            protected void visitExecutable(ExecutableElement executableElement) {
                builder.addStatement("m.put($S, $T.$$generateSerializer())",
                        getIceClass(executableElement),
                        getIceClassName(executableElement));
            }
        };
        for (Element e : element.getEnclosedElements()) {
            visitor.visit(e);
        }
        builder.addStatement("return m");
        return builder.build();
    }

    private ParameterizedTypeName serializersMap() {
        ClassName serializedRegistryRawType = ClassName.get(ChioneSerializer.class);
        WildcardTypeName wildcard = WildcardTypeName.subtypeOf(Object.class);
        ParameterizedTypeName serializedRegistry = ParameterizedTypeName.get(serializedRegistryRawType, wildcard);

        return ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                serializedRegistry
        );
    }

    private ParameterizedTypeName chioneWrapper(TypeElement element) {
        ClassName rawType = ClassName.get(ChioneWrapper.class);
        TypeName rootClass = rootClass(element);
        return ParameterizedTypeName.get(rawType, rootClass);
    }

    private ParameterizedTypeName moduleType(TypeElement element) {
        ClassName rawType = ClassName.get(ChioneModule.class);
        TypeName root = rootClass(element);
        ClassName factoryClass = factoryClass(element);
        return ParameterizedTypeName.get(rawType, root, factoryClass);
    }

    private TypeName rootClass(TypeElement element) {
        try {
            return ClassName.get(element.getAnnotation(Factory.class).root());
        } catch (MirroredTypeException e) {
            return ClassName.get(e.getTypeMirror());
        }
    }

    private ClassName factoryClass(TypeElement element) {
        return ClassName.get(element);
    }

    private TypeSpec iceFactory(TypeElement element) {
        ClassName className = factoryClass(element);

        TypeSpec.Builder builder = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(className);

        VoidVisitor visitor = new VoidVisitor() {
            @Override
            protected void visitExecutable(ExecutableElement executableElement) {
                builder.addMethod(generate(executableElement));
            }
        };
        for (Element e : element.getEnclosedElements()) {
            visitor.visit(e);
        }
        return builder.build();
    }

    private MethodSpec generate(ExecutableElement element) {
        MethodSpec.Builder method = MethodSpec.overriding(element);

        ClassName implClassName = getGeneratedClassName(element);
        method.addCode("return new $T();\n", implClassName);
        return method.build();
    }

    private TypeMirror getIceClass(ExecutableElement element) {
        return getIceClassOfElement(getReturnType(element).asElement());
    }

    private DeclaredType getReturnType(ExecutableElement element) {
        return (DeclaredType)element.getReturnType();
    }

    private ClassName getIceClassName(ExecutableElement element) {
        DeclaredType iceClass = (DeclaredType) getIceClass(element);
        return getGeneratedName(iceClass);
    }

    private ClassName getGeneratedClassName(ExecutableElement element) {
        DeclaredType returnType = getReturnType(element);
        return getGeneratedName(returnType);
    }

    private ClassName getGeneratedName(DeclaredType returnType) {
        ClassName className = ClassName.get((TypeElement) returnType.asElement());
        return IceProcessor.getGeneratedName(className);
    }

    private TypeMirror getIceClassOfElement(Element element) {
        if (element.getAnnotation(Ice.class) != null) {
            return element.asType();
        }
        if (element.getAnnotation(Ice.Builder.class) != null) {
            return getIceClassOfElement(element.getEnclosingElement());
        }
        throw new IllegalArgumentException("Type " + element + " has no @Ice annotation");
    }
}
