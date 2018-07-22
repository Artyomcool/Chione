package com.github.artyomcool.chione;

import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"com.github.artyomcool.chione.Ice", "com.github.artyomcool.chione.Factory"})
public class AnnotationProcessor extends AbstractProcessor {

    private final List<JavaFile> generatedSpecs = new ArrayList<>();

    private final List<Processor<?>> processors = Arrays.asList(
            new IceProcessor(),
            new FactoryProcessor()
    );

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (generatedSpecs.isEmpty()) {
                return false;
            }

            for (JavaFile file : generatedSpecs) {

                try {
                    file.writeTo(processingEnv.getFiler());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            //TODO generate registry
            return false;
        }

        int oldSize = generatedSpecs.size();

        for (Processor<?> processor : processors) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(processor.supportedAnnotation());
            generatedSpecs.addAll(processor.process(elements));
        }

        return generatedSpecs.size() > oldSize;
    }


}
