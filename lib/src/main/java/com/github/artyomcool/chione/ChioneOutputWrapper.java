/*
 * The MIT License
 *
 * Copyright (c) 2018 Artyom Drozdov (https://github.com/artyomcool)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
