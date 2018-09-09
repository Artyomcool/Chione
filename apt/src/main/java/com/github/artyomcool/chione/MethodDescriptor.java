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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

class MethodDescriptor {

    private final String name;
    private final ExecutableElement getter;
    private final ExecutableElement setter;
    private final TypeName originalType;
    private final TypeName type;
    private final Fetch.Type fetchType;

    MethodDescriptor(String name, ExecutableElement getter, ExecutableElement setter) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;

        this.originalType = TypeName.get(getter.getReturnType());
        this.fetchType = extractFetch();
        this.type = extractType();
    }

    public String name() {
        return name;
    }

    public ExecutableElement getter() {
        return getter;
    }

    public ExecutableElement setter() {
        return setter;
    }

    public TypeName type() {
        return type;
    }

    private TypeName extractType() {
        if (fetchType == Fetch.Type.EAGER) {
            return originalType;
        } else {
            return ParameterizedTypeName.get(ClassName.get(Lazy.class), originalType.box());
        }
    }

    private Fetch.Type extractFetch() {
        if (getter == null && setter == null) {
            throw new IllegalArgumentException("Both setter and getter of '" + name + "' are null");
        }
        Fetch getterFetch = getter == null ? null : getter.getAnnotation(Fetch.class);
        Fetch setterFetch = setter == null ? null : setter.getAnnotation(Fetch.class);

        if (getterFetch == null && setterFetch == null) {
            if (originalType.isPrimitive()) {
                return Fetch.Type.EAGER;
            }
            Element element = getter == null ? setter : getter;
            return traverseParentForFetch(element.getEnclosingElement());
        }

        if (setterFetch == null) {
            return getterFetch.value();
        }

        if (getterFetch == null) {
            return setterFetch.value();
        }

        Fetch.Type value = getterFetch.value();
        if (value == setterFetch.value()) {
            return value;
        }

        throw new IllegalStateException("Getter and setter of '" + name + "' has different fetch type");
    }

    private Fetch.Type traverseParentForFetch(Element element) {
        if (element == null) {
            return Fetch.Type.EAGER;
        }
        Fetch annotation = element.getAnnotation(Fetch.class);
        if (annotation != null) {
            return annotation.value();
        }
        return traverseParentForFetch(element.getEnclosingElement());
    }

    public CodeBlock fieldAccessor() {
        return fetchType == Fetch.Type.EAGER
                ? CodeBlock.of("return this.$L", name)
                : CodeBlock.of("return this.$L.get()", name);
    }

    public CodeBlock fieldInitializer() {
        return fetchType == Fetch.Type.EAGER
                ? null
                : CodeBlock.of("new $T<>($L)", Lazy.class, defaultValue());
    }

    public CodeBlock fieldMutator() {
        String firstParamName = setter.getParameters().get(0).getSimpleName().toString();

        return fetchType == Fetch.Type.EAGER
                ? CodeBlock.of("this.$L = $L", name, firstParamName)
                : CodeBlock.of("this.$L.set($L)", name, firstParamName);

    }

    private String defaultValue() {
        if (!originalType.isPrimitive()) {
            return "null";
        }
        String type = originalType.toString();
        switch (type) {
            case "boolean":
                return "false";
            case "byte":
            case "short":
            case "char":
                return "("+type+") 0";
            case "long":
                return "0L";
            case "int":
                return "0";
            case "float":
                return "0f";
            case "double":
                return "0.0";
            default:
                throw new IllegalArgumentException("Unknown primitive:" + type);
        }
    }
}
