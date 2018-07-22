package com.github.artyomcool.chione;

import java.util.List;

public class ReadOnlyArray<T> {

    private final T[] data;

    public ReadOnlyArray(T[] data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public static <T> ReadOnlyArray<T> fromList(List<T> steps) {
        Object[] objects = steps.toArray();
        return (ReadOnlyArray<T>) new ReadOnlyArray<>(objects);
    }

    public T get(int index) {
        return data[index];
    }

    public int size() {
        return data.length;
    }

}
