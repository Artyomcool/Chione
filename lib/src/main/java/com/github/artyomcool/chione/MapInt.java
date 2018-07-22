package com.github.artyomcool.chione;

import java.util.Map;

abstract class MapInt<K> {

    private static final int HASH_TRY_COUNT = 3;

    private final float loadFactor;

    private Object[] tableKeys;
    private int[] tableValues;
    private int load;
    private int resizeAt;

    private final Map<K, Integer> collisionTable;

    MapInt(int initialCapacity, float loadFactor) {
        this.loadFactor = loadFactor;

        int tableSize = (int) (initialCapacity / loadFactor);
        this.tableKeys = new Object[tableSize];
        this.tableValues = new int[tableSize];
        this.load = 0;
        this.resizeAt = initialCapacity;

        //TODO resolve collision through in-place secondary hash/tree
        this.collisionTable = allocateCollisionTable(initialCapacity / 2, loadFactor);
    }

    public int putIfAbsent(K element, ReferenceAllocator allocator) {
        int hash = getHash(element);  //TODO murmur?

        int index = -1;
        Object key = null;

        for (int t = 0; t < HASH_TRY_COUNT; t++) {
            index = ((hash + t) & 0x7fffffff) % tableKeys.length;
            key = tableKeys[index];
            @SuppressWarnings("unchecked")
            K castedKey = (K) key;
            if (equals(element, castedKey)) {
                return tableValues[index];
            }
            if (key == null) {
                break;
            }
        }

        Integer integer = collisionTable.get(element);
        if (integer != null) {
            return integer;
        }

        int value = allocator.nextReference();

        if (key == null) {
            tableKeys[index] = element;
            tableValues[index] = value;
            if (++load >= resizeAt) {
                resize(resizeAt * 3 / 2 + 1);
            }
        } else {
            collisionTable.put(element, value);
        }

        return ~value;
    }

    abstract int getHash(K element);

    abstract boolean equals(K e1, K e2);

    abstract Map<K, Integer> allocateCollisionTable(int initialCapacity, float loadFactor);

    private void resize(int newResizeAt) {
        int tableSize = (int) (newResizeAt / loadFactor);

        Object[] newKeys = new Object[tableSize];
        int[] newValues = new int[tableSize];

        Object[] oldKeys = this.tableKeys;
        int[] oldValues = this.tableValues;

        nextKey: for (int i = 0; i < oldKeys.length; i++) {
            Object key = oldKeys[i];
            if (key == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            int hash = getHash((K) key);
            int value = oldValues[i];

            for (int t = 0; t < HASH_TRY_COUNT; t++) {
                int index = (hash & 0x7fffffff) % newKeys.length;
                Object collidedKey = newKeys[index];
                if (collidedKey == null) {
                    newKeys[index] = key;
                    newValues[index] = value;
                    continue nextKey;
                }
            }

            @SuppressWarnings("unchecked")
            K typedKey = (K) key;
            collisionTable.put(typedKey, value);
        }

        resizeAt = newResizeAt;
        tableKeys = newKeys;
        tableValues = newValues;
    }

}
