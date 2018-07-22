package com.github.artyomcool.chione;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class ReferenceAllocator {

    private int reference;

    public int nextReference() {
        return reference++;
    }

}
