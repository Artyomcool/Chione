package com.github.artyomcool.chione;

import java.util.IdentityHashMap;
import java.util.Map;

class IdentityHashMapInt<K> extends MapInt<K> {

    IdentityHashMapInt(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    @Override
    int getHash(K element) {
        return System.identityHashCode(element);
    }

    @Override
    boolean equals(K e1, K e2) {
        return e1 == e2;
    }

    @Override
    Map<K, Integer> allocateCollisionTable(int initialCapacity, float loadFactor) {
        return new IdentityHashMap<>();
    }
}
