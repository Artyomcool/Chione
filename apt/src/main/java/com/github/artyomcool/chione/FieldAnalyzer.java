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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.*;

class FieldAnalyzer {

    private static final String SETTER_PREFIX = "set";
    private static final String GETTER_PREFIX = "get";
    private static final String GETTER_PREFIX_FOR_BOOLEAN = "is";

    private final Map<String, ExecutableElement> getters = new HashMap<>();
    private final Map<String, ExecutableElement> setters = new HashMap<>();
    private final List<MethodDescriptor> methods = new ArrayList<>();

    private FieldAnalyzer() {
    }

    static List<MethodDescriptor> analyze(TypeElement element) {
        return analyze(element, element);
    }

    static List<MethodDescriptor> analyze(TypeElement forGetters, TypeElement forSetters) {
        FieldAnalyzer analyzer = new FieldAnalyzer();
        analyzer.collect(forGetters, forSetters);
        analyzer.sameNameStep();
        analyzer.beanStyleStep();
        analyzer.gettersOnlyStep();
        analyzer.verifyNothingLeftStep();
        return analyzer.methods;
    }

    private void collect(TypeElement forGetters, TypeElement forSetters) {
        VoidVisitor getterVisitor = new VoidVisitor() {
            @Override
            protected void visitExecutable(ExecutableElement e) {
                if (e.isDefault()) {
                    return;
                }

                if (e.getParameters().size() == 0) {
                    getters.put(e.getSimpleName().toString(), e);
                }
            }
        };
        VoidVisitor setterVisitor = new VoidVisitor() {
            @Override
            protected void visitExecutable(ExecutableElement e) {
                if (e.isDefault()) {
                    return;
                }

                if (e.getParameters().size() == 1) {
                    setters.put(e.getSimpleName().toString(), e);
                }
            }
        };

        for (Element enclosed : forGetters.getEnclosedElements()) {
            getterVisitor.visit(enclosed);
        }

        for (Element enclosed : forSetters.getEnclosedElements()) {
            setterVisitor.visit(enclosed);
        }
    }

    private void sameNameStep() {
        Iterator<Map.Entry<String, ExecutableElement>> iterator = getters.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<String, ExecutableElement> nextGetterEntry = iterator.next();
            String getterName = nextGetterEntry.getKey();
            ExecutableElement nextGetter = nextGetterEntry.getValue();

            ExecutableElement nextSetter = setters.remove(getterName);

            if (nextSetter != null) {
                MethodDescriptor relatedMethods = new MethodDescriptor(getterName, nextGetter, nextSetter);
                methods.add(relatedMethods);
                iterator.remove();
            }
        }
    }

    private void beanStyleStep() {
        Iterator<Map.Entry<String, ExecutableElement>> iterator = setters.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<String, ExecutableElement> nextSetterEntry = iterator.next();

            String setterName = nextSetterEntry.getKey();
            ExecutableElement nextSetter = nextSetterEntry.getValue();

            int setterPrefixLength = SETTER_PREFIX.length();
            if (setterName.length() <= setterPrefixLength) {
                continue;
            }

            if (!setterName.startsWith(SETTER_PREFIX)) {
                continue;
            }

            char firstLetter = setterName.charAt(setterPrefixLength);
            if (!Character.isUpperCase(firstLetter)) {
                continue;
            }

            String fieldNameCapitalized = setterName.substring(setterPrefixLength);

            ExecutableElement nextGetter = getters.remove(GETTER_PREFIX + fieldNameCapitalized);
            if (nextGetter == null) {
                nextGetter = getters.remove(GETTER_PREFIX_FOR_BOOLEAN + fieldNameCapitalized);
            }

            if (nextGetter != null) {
                String name = Character.toLowerCase(firstLetter) + fieldNameCapitalized.substring(1);

                MethodDescriptor relatedMethods = new MethodDescriptor(name, nextGetter, nextSetter);
                methods.add(relatedMethods);
                iterator.remove();
            }
        }
    }

    private void gettersOnlyStep() {
        for (Map.Entry<String, ExecutableElement> entry : getters.entrySet()) {
            methods.add(new MethodDescriptor(entry.getKey(), entry.getValue(), null));
        }
        getters.clear();
    }

    private void verifyNothingLeftStep() {
        //TODO
    }

}
