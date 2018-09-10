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
import javax.lang.model.type.TypeKind;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static javax.lang.model.element.Modifier.*;

class IceProcessor extends Processor<Ice> {

    static final String GENERATED_IMPLEMENTATION_SUFFIX = "$$$Impl";

    IceProcessor() {
        super(Ice.class);
    }

    @Override
    protected JavaFile generate(TypeElement element) {
        if (!element.getKind().isInterface()) {
            throw new IllegalArgumentException("@Ice supports only interfaces");
        }

        ClassName className = ClassName.get(element);
        ClassName generatedClassName = ClassName.get(
                className.packageName(),
                className.simpleName() + GENERATED_IMPLEMENTATION_SUFFIX
        );
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(generatedClassName)
                .addModifiers(PUBLIC)
                .superclass(SnowFlake.class)
                .addSuperinterface(className);

        List<MethodDescriptor> methods = FieldAnalyzer.analyze(element);
        Optional<? extends Element> builder = element.getEnclosedElements().stream()
                .filter(e -> e.getAnnotation(Ice.Builder.class) != null)
                .findAny();

        if (builder.isPresent()) {
            TypeElement builderElement = (TypeElement) builder.get();
            ClassName builderType = ClassName.get(builderElement);

            typeSpecBuilder.addMethod(MethodSpec.constructorBuilder().build());
            typeSpecBuilder.addMethod(MethodSpec.constructorBuilder()
                    .addParameter(getGeneratedName(builderType), "builder")
                    .addCode(builderConstructor(FieldAnalyzer.analyze(element, builderElement)))
                    .build());
        }

        for (MethodDescriptor descriptor : methods) {
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(descriptor.type(), descriptor.name(), PRIVATE);
            CodeBlock initializer = descriptor.fieldInitializer();
            if (initializer != null) {
                fieldBuilder.initializer(initializer);
            }
            typeSpecBuilder.addField(fieldBuilder.build());
        }

        for (MethodDescriptor descriptor : methods) {
            ExecutableElement getter = descriptor.getter();
            if (getter != null) {
                MethodSpec getterSpec = MethodSpec.overriding(getter)
                        .addStatement(getterBody(descriptor))
                        .build();

                typeSpecBuilder.addMethod(getterSpec);
            }


            ExecutableElement setter = descriptor.setter();
            if (setter != null) {
                MethodSpec.Builder methodSpecBuilder = MethodSpec.overriding(setter)
                        .addStatement(setterBody(descriptor));

                if (setter.getReturnType().getKind() != TypeKind.VOID) {
                    methodSpecBuilder.addStatement("return this");
                }

                typeSpecBuilder.addMethod(methodSpecBuilder.build());
            }
        }

        methods.sort(Comparator.comparing(MethodDescriptor::name));

        MethodSpec generateSerializer = MethodSpec.methodBuilder("$generateSerializer")
                .addModifiers(PUBLIC, STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(SnowFlakeSerializer.class), generatedClassName))
                .addCode(generateSerializerCode(methods, className, generatedClassName))
                .build();

        typeSpecBuilder.addMethod(generateSerializer);

        MethodSpec write = MethodSpec.methodBuilder("write")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ChioneDataOutput.class, "output")
                .addCode(writeCode(methods))
                .build();

        typeSpecBuilder.addMethod(write);

        MethodSpec readTrivial = MethodSpec.methodBuilder("readTrivial")
                .addModifiers(PROTECTED)
                .addAnnotation(Override.class)
                .addParameter(ChioneDataInput.class, "input")
                .addCode(readCode(methods, false))
                .build();

        typeSpecBuilder.addMethod(readTrivial);

        MethodSpec readSimple = MethodSpec.methodBuilder("readSimple")
                .addModifiers(PROTECTED)
                .addAnnotation(Override.class)
                .addParameter(ChioneDataInput.class, "input")
                .addParameter(ParameterizedTypeName.get(ReadOnlyArray.class, Step.class), "steps")
                .addCode(readCode(methods, true))
                .build();

        typeSpecBuilder.addMethod(readSimple);

        MethodSpec chioneName = MethodSpec.methodBuilder("chioneName")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $S", className.reflectionName())
                .build();

        typeSpecBuilder.addMethod(chioneName);

        TypeSpec typeSpec = typeSpecBuilder.build();

        return JavaFile.builder(className.packageName(), typeSpec)
                .skipJavaLangImports(true)
                .build();
    }

    private CodeBlock getFieldsInitializer(List<MethodDescriptor> descriptors) {
        CodeBlock snowFlakes = descriptors
                .stream()
                .map(this::toSnowFlakeField)
                .collect(CodeBlock.joining(",\n"));

        return CodeBlock.of("$T.asList(\n$>$>$L\n$<$<)", Arrays.class, snowFlakes);
    }

    private CodeBlock toSnowFlakeField(MethodDescriptor descriptor) {
        return CodeBlock.builder()
                .add(
                        "new $T($S, $S)",
                        SnowFlakeField.class,
                        descriptor.name(),
                        descriptor.type().toString()
                )
                .build();
    }

    private CodeBlock generateSerializerCode(List<MethodDescriptor> methods, ClassName className, ClassName generatedClassName) {

        TypeSpec serializer = TypeSpec.anonymousClassBuilder("new $T($S, $L, $L)", ChioneDescriptor.class, className, 1, "fields")
                .superclass(ParameterizedTypeName.get(ClassName.get(SnowFlakeSerializer.class), generatedClassName))
                .addMethod(
                        MethodSpec.methodBuilder("instantiate")
                                .addModifiers(PROTECTED)
                                .addAnnotation(Override.class)
                                .returns(generatedClassName)
                                .addCode("return new $T();\n", generatedClassName)
                                .build()
                )
                .build();

        return CodeBlock.builder()
                .addStatement("$T fields = $L", ParameterizedTypeName.get(List.class, SnowFlakeField.class), getFieldsInitializer(methods))
                .add("\n")
                .addStatement("return $L", serializer)
                .build();
    }

    private CodeBlock writeCode(List<MethodDescriptor> descriptors) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (MethodDescriptor descriptor : descriptors) {
            TypeName type = descriptor.type();
            if (!type.isPrimitive()) {
                builder.addStatement("output.writeReference(this.$L)", descriptor.name());
            } else {
                if (type == TypeName.BOOLEAN) {
                    builder.addStatement("output.write(this.$L ? (byte) 1 : (byte) 0)", descriptor.name());
                } else if (type == TypeName.CHAR) {
                    builder.addStatement("output.write((short) this.$L)", descriptor.name());
                } else if (type == TypeName.BYTE
                        || type == TypeName.SHORT
                        || type == TypeName.INT
                        || type == TypeName.LONG) {
                    builder.addStatement("output.write(this.$L)", descriptor.name());
                } else if (type == TypeName.FLOAT) {
                    builder.addStatement("output.write(Float.floatToRawIntBits(this.$L))", descriptor.name());
                } else if (type == TypeName.DOUBLE) {
                    builder.addStatement("output.write(Double.doubleToRawLongBits(this.$L))", descriptor.name());
                } else {
                    throw new AssertionError();
                }
            }
        }
        return builder.build();
    }

    private CodeBlock readCode(List<MethodDescriptor> descriptors, boolean withSteps) {
        CodeBlock.Builder builder = CodeBlock.builder();
        if (withSteps) {
            builder.addStatement("int current = -1");
        }
        for (MethodDescriptor descriptor : descriptors) {
            if (withSteps) {
                builder.addStatement("current = skip(input, steps, current)");
                builder.beginControlFlow("if (hasCurrentField(steps, current))");
            }
            TypeName type = descriptor.type();
            if (!type.isPrimitive()) {
                builder.addStatement("this.$L = input.readReference()", descriptor.name());
            } else {
                if (type == TypeName.BOOLEAN) {
                    builder.addStatement("this.$L = input.readByte() > 0", descriptor.name());
                } else if (type == TypeName.BYTE) {
                    builder.addStatement("this.$L = input.readByte()", descriptor.name());
                } else if (type == TypeName.SHORT) {
                    builder.addStatement("this.$L = input.readShort()", descriptor.name());
                } else if (type == TypeName.CHAR) {
                    builder.addStatement("this.$L = (char) input.readShort()", descriptor.name());
                } else if (type == TypeName.INT) {
                    builder.addStatement("this.$L = input.readInt()", descriptor.name());
                } else if (type == TypeName.LONG) {
                    builder.addStatement("this.$L = input.readLong()", descriptor.name());
                } else if (type == TypeName.FLOAT) {
                    builder.addStatement("this.$L = Float.intBitsToFloat(input.readInt())", descriptor.name());
                } else if (type == TypeName.DOUBLE) {
                    builder.addStatement("this.$L = Double.longBitsToDouble(input.readLong())", descriptor.name());
                } else {
                    throw new AssertionError();
                }
            }
            if (withSteps) {
                builder.endControlFlow();
            }
        }
        return builder.build();
    }

    private CodeBlock getterBody(MethodDescriptor descriptor) {
        return descriptor.fieldAccessor();
    }

    private CodeBlock setterBody(MethodDescriptor descriptor) {
        return descriptor.fieldMutator();
    }

    private CodeBlock builderConstructor(List<MethodDescriptor> methods) {
        CodeBlock.Builder result = CodeBlock.builder();
        for (MethodDescriptor descriptor : methods) {
            if (descriptor.setter() != null) {
                result.addStatement(descriptor.fieldMutator("builder." + descriptor.name()));
            }
        }
        return result.build();
    }

    static ClassName getGeneratedName(ClassName className) {
        String name = String.join("$", className.simpleNames()) + IceProcessor.GENERATED_IMPLEMENTATION_SUFFIX;
        return ClassName.bestGuess(name);
    }

}
