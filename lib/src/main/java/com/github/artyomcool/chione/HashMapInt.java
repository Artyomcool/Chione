package com.github.artyomcool.chione;

import java.util.HashMap;
import java.util.Map;

class HashMapInt<K> extends MapInt<K> {

    HashMapInt(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    @Override
    int getHash(K element) {
        return element.hashCode();
    }

    @Override
    boolean equals(K e1, K e2) {
        return e1.equals(e2);
    }

    @Override
    Map<K, Integer> allocateCollisionTable(int initialCapacity, float loadFactor) {
        return new HashMap<>();
    }
}
