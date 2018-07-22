package com.github.artyomcool.chione;

public class ArrayMap<K, V> {

    private Object[] storage = new Object[8];
    private int count = 0;

    public V get(K key) {
        for (int i = 0; i < count; i++) {
            if (key.equals(storage[i])) {
                @SuppressWarnings("unchecked")
                V result = (V) storage[storage.length / 2 + i];
                return result;
            }
        }
        return null;
    }

    public void put(K key, V value) {
        int valuePos = count * 2;
        storage[valuePos] = value;
        storage[count++] = key;
    }

}
