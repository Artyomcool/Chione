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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Type;
import java.util.*;

import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.*;

class IceBuilderProcessor extends Processor<Ice.Builder> {

    IceBuilderProcessor() {
        super(Ice.Builder.class);
    }

    @Override
    protected JavaFile generate(TypeElement element) {
        if (!element.getKind().isInterface()) {
            throw new IllegalArgumentException("@Ice supports only interfaces");
        }

        ClassName className = ClassName.get(element);

        String name = String.join("$", className.simpleNames());
        ClassName generatedClassName = ClassName.get(
                className.packageName(),
                name + IceProcessor.GENERATED_IMPLEMENTATION_SUFFIX
        );
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(generatedClassName)
                .addModifiers(PUBLIC)
                .addSuperinterface(className);

        List<MethodDescriptor> methods = FieldAnalyzer.analyze((TypeElement) element.getEnclosingElement(), element);

        for (MethodDescriptor descriptor : methods) {
            FieldSpec.Builder builder = FieldSpec.builder(descriptor.originalType(), descriptor.name());
            typeSpecBuilder.addField(builder.build());
        }

        for (MethodDescriptor descriptor : methods) {
            ExecutableElement setter = descriptor.setter();
            if (setter != null) {
                MethodSpec setterSpec = MethodSpec.overriding(setter)
                        .addCode(setterBody(descriptor))
                        .build();

                typeSpecBuilder.addMethod(setterSpec);
            }
        }

        Optional<? extends Element> build = element.getEnclosedElements().stream().filter(
                e -> e.getKind() == METHOD && e.getSimpleName().toString().equals("build")
        ).findFirst();

        if (build.isPresent()) {
            ClassName entry = ClassName.get((TypeElement) element.getEnclosingElement());
            MethodSpec getterSpec = MethodSpec.overriding((ExecutableElement) build.get())
                    .addStatement("return new $T(this)", IceProcessor.getGeneratedName(entry))
                    .build();

            typeSpecBuilder.addMethod(getterSpec);
        }

        TypeSpec typeSpec = typeSpecBuilder.build();

        return JavaFile.builder(className.packageName(), typeSpec)
                .skipJavaLangImports(true)
                .build();
    }

    private CodeBlock setterBody(MethodDescriptor methodDescriptor) {
        VariableElement firstParam = methodDescriptor.setter().getParameters().get(0);
        String firstParamName = firstParam.getSimpleName().toString();

        CodeBlock.Builder result = CodeBlock.builder();
        result.addStatement("this.$L = $L", methodDescriptor.name(), firstParamName);
        if (methodDescriptor.setter().getReturnType().getKind() != TypeKind.VOID) {
            result.addStatement("return this");
        }
        return result.build();
    }

}
