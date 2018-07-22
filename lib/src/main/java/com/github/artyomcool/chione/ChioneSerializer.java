package com.github.artyomcool.chione;

public interface ChioneSerializer<T> {

    ChioneDescriptor describe(T obj);

    void writeContent(T obj, ChioneDataOutput dataOutput);

    T deserialize(DeserializationContext context);

}
