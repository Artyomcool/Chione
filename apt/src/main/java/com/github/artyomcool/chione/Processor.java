package com.github.artyomcool.chione;

import com.squareup.javapoet.JavaFile;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

abstract class Processor<T extends Annotation> {

    private final Class<T> annotation;

    protected Processor(Class<T> annotation) {
        this.annotation = annotation;
    }

    public Class<T> supportedAnnotation() {
        return annotation;
    }

    public List<JavaFile> process(Iterable<? extends Element> elements) {
        List<JavaFile> generatedSpecs = new ArrayList<>();

        VoidVisitor typeGenerateVisitor = new VoidVisitor() {
            @Override
            protected void visitType(TypeElement type) {
                JavaFile javaFile = generate(type);
                generatedSpecs.add(javaFile);
            }
        };

        for (Element element : elements) {
            typeGenerateVisitor.visit(element);
        }

        return generatedSpecs;
    }

    protected abstract JavaFile generate(TypeElement element);

}
