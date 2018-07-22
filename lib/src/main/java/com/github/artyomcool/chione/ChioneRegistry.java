package com.github.artyomcool.chione;

import java.util.ArrayDeque;
import java.util.Deque;

public class ChioneRegistry implements ReferenceRegistry {

    private final Deque<Object> remainsToWrite = new ArrayDeque<>(32);
    private final ReferenceAllocator referenceAllocator = new ReferenceAllocator();
    private final Recollection<Object> recollection;

    public ChioneRegistry(Recollection<Object> recollection) {
        this.recollection = recollection;
    }

    @Override
    public int ref(Object obj) {
        if (obj == null) {
            return NULL_REF;
        }

        int id = recollection.remember(obj, referenceAllocator);

        if (id >= 0) {
            return id;
        }

        remainsToWrite.push(obj);
        return ~id;
    }

    public Object poll() {
        return remainsToWrite.pollLast();
    }

}
