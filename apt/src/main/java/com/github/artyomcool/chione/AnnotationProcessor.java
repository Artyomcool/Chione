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
            new IceBuilderProcessor(),
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
