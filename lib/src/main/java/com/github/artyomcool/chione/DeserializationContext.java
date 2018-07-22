package com.github.artyomcool.chione;

public interface DeserializationContext {

    ChioneDataInput input();

    ChioneDescriptor descriptor();

    <T> T hookCreation(T obj);

}
