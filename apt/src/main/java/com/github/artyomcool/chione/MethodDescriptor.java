package com.github.artyomcool.chione;

import javax.lang.model.element.ExecutableElement;

class MethodDescriptor {
    private final String name;
    private final ExecutableElement getter;
    private final ExecutableElement setter;

    MethodDescriptor(String name, ExecutableElement getter, ExecutableElement setter) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;
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
}
