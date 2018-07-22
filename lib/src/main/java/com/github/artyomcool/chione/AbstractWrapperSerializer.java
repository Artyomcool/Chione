package com.github.artyomcool.chione;

abstract class AbstractWrapperSerializer<T> extends AbstractChioneSerializer<T> {

    AbstractWrapperSerializer(String className) {
        super(className);
    }

    @Override
    public T deserialize(DeserializationContext context) {
        T obj = read(context.input());
        return context.hookCreation(obj);
    }

    abstract T read(ChioneDataInput input);

}
