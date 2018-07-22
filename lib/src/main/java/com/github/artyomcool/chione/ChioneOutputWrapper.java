package com.github.artyomcool.chione;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class ChioneOutputWrapper implements ChioneDataOutput {

    private final DataOutput delegate;
    private final ReferenceRegistry registry;

    private int pos = 0;

    public ChioneOutputWrapper(DataOutput delegate,
                               ReferenceRegistry registry) {
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public int writeReference(Object reference) {
        return write(registry.ref(reference));
    }

    @Override
    public int currentOffset() {
        return pos;
    }

    @Override
    public int write(byte b) {
        return incPos(delegate.write(b));
    }

    @Override
    public int write(short s) {
        return incPos(delegate.write(s));
    }

    @Override
    public int write(int i) {
        return incPos(delegate.write(i));
    }

    @Override
    public int write(long l) {
        return incPos(delegate.write(l));
    }

    @Override
    public int write(String s) {
        return incPos(delegate.write(s));
    }

    @Override
    public int write(byte[] data) {
        return incPos(delegate.write(data));
    }

    private int incPos(int size) {
        pos += size;
        return size;
    }
}
