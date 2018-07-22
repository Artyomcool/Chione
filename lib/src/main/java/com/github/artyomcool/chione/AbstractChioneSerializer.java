package com.github.artyomcool.chione;

abstract class AbstractChioneSerializer<T> implements ChioneSerializer<T> {

    private final String className;

    AbstractChioneSerializer(String className) {
        this.className = className;
    }

    @Override
    public final ChioneDescriptor describe(T obj) {
        return new ChioneDescriptor(className);
    }

    String getClassName() {
        return className;
    }
}
