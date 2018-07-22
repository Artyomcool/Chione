package com.github.artyomcool.chione;

import com.squareup.javapoet.*;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.*;

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

        for (MethodDescriptor descriptor : methods) {
            TypeName typeName = TypeName.get(descriptor.getter().getReturnType());
            typeSpecBuilder.addField(typeName, descriptor.name(), PRIVATE);
        }

        for (MethodDescriptor descriptor : methods) {
            MethodSpec getterSpec = MethodSpec.overriding(descriptor.getter())
                    .addStatement("return this.$L", descriptor.name())
                    .build();

            typeSpecBuilder.addMethod(getterSpec);

            ExecutableElement setter = descriptor.setter();
            String firstParamName = setter.getParameters().get(0).getSimpleName().toString();

            MethodSpec.Builder methodSpecBuilder = MethodSpec.overriding(setter)
                    .addStatement("this.$L = $L", descriptor.name(), firstParamName);

            if (setter.getReturnType().getKind() != TypeKind.VOID) {
                methodSpecBuilder.addStatement("return this");
            }

            typeSpecBuilder.addMethod(methodSpecBuilder.build());
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
                        descriptor.getter().getReturnType().toString()
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
            switch (descriptor.getter().getReturnType().getKind()) {
                case ARRAY:
                case DECLARED:
                    builder.addStatement("output.writeReference(this.$L)", descriptor.name());
                    break;
                case BOOLEAN:
                    builder.addStatement("output.write(this.$L ? 1 : 0)", descriptor.name());
                    break;
                default:
                    builder.addStatement("output.write(this.$L)", descriptor.name());
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
            switch (descriptor.getter().getReturnType().getKind()) {
                case ARRAY:
                case DECLARED:
                    builder.addStatement("this.$L = input.readReference()", descriptor.name());
                    break;
                case BOOLEAN:
                    builder.addStatement("this.$L = input.readByte() > 0", descriptor.name());
                    break;
                case BYTE:
                    builder.addStatement("this.$L = input.readByte()", descriptor.name());
                    break;
                case SHORT:
                    builder.addStatement("this.$L = input.readShort()", descriptor.name());
                    break;
                case INT:
                    builder.addStatement("this.$L = input.readInt()", descriptor.name());
                    break;
                case LONG:
                    builder.addStatement("this.$L = input.readLong()", descriptor.name());
                    break;
                case FLOAT:
                    builder.addStatement("this.$L = input.readFloat()", descriptor.name());
                    break;
                case DOUBLE:
                    builder.addStatement("this.$L = input.readDouble()", descriptor.name());
                    break;
            }
            if (withSteps) {
                builder.endControlFlow();
            }
        }
        return builder.build();
    }

}
