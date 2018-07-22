package com.github.artyomcool.chione;

public interface Recollection<T> {

    int remember(T obj, ReferenceAllocator referenceAllocator);

}
