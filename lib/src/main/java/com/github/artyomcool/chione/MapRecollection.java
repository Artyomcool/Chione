package com.github.artyomcool.chione;

public class MapRecollection<T> implements Recollection<T> {

    private final MapInt<T> map;

    public MapRecollection(MapInt<T> map) {
        this.map = map;
    }

    @Override
    public int remember(T obj, ReferenceAllocator referenceAllocator) {
        return map.putIfAbsent(obj, referenceAllocator);
    }
}
