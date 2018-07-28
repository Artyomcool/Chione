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

import java.util.ArrayDeque;
import java.util.Deque;

import static com.github.artyomcool.chione.ReferenceRegistry.NULL_REF;
import static com.github.artyomcool.chione.Util.unsafeCast;

public class ChioneInputWrapper implements ChioneDataInput {

    private final ChioneSerializer<Object> serializer;
    private final DataInput delegate;
    private final int[] objectOffsets;  //TODO no need to store it
    private final Object[] objects;    //TODO more compact way?
    private final ContextPool contextPool = new ContextPool();
    private final ObjectInflater objectInflater = new ObjectInflater();
    private final DescriptorInflater descriptorInflater = new DescriptorInflater();

    public ChioneInputWrapper(ChioneSerializer<Object> serializer, DataInput delegate, int[] objectOffsets) {
        this.serializer = serializer;
        this.delegate = delegate;
        this.objectOffsets = objectOffsets;

        objects = new Object[objectOffsets.length];
    }

    @Override
    public <T> T readReference() {
        int ref = readInt();
        if (ref == NULL_REF) {
            return null;
        }
        return unsafeCast(objectFromRef(ref));
    }

    @Override
    public void seek(int offset) {
        delegate.seek(offset);
    }

    @Override
    public int pos() {
        return delegate.pos();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public byte readByte() {
        return delegate.readByte();
    }

    @Override
    public short readShort() {
        return delegate.readShort();
    }

    @Override
    public int readInt() {
        return delegate.readInt();
    }

    @Override
    public long readLong() {
        return delegate.readLong();
    }

    @Override
    public String readString() {
        return delegate.readString();
    }

    private Object objectFromRef(int ref) {
        return objectFromRef(ref, objectInflater);
    }

    private ChioneDescriptor readDescriptor() {
        int ref = readInt();
        if (ref == StringSerializer.DESCRIPTOR_STATIC_REFERENCE) { //TODO
            return StringSerializer.DESCRIPTOR; //TODO
        }
        return objectFromRef(ref, descriptorInflater);
    }

    private <T> T objectFromRef(int ref, Inflater<T> inflater) {
        Object cached = objects[ref];
        if (cached != null) {
            return unsafeCast(cached);
        }

        int oldOffset = pos();
        seek(objectOffsets[ref]);

        T result = inflater.inflate(ref);

        seek(oldOffset);
        return result;
    }

    private ChioneDescriptor inflateDescriptor(int ref) {
        ChioneDescriptor descriptor = new ChioneDescriptor();
        objects[ref] = descriptor;

        ChioneDescriptor descriptorDescriptor = readDescriptor();
        descriptor.read(ChioneInputWrapper.this, descriptorDescriptor);

        return descriptor;
    }

    private Object inflateObject(int ref) {
        ChioneDescriptor descriptor = readDescriptor();
        WrapperDeserializationContext context = contextPool.get(descriptor, ref);
        Object result = serializer.deserialize(context);
        contextPool.recycle(context);
        return result;
    }

    private interface Inflater<T> {
        T inflate(int ref);
    }

    private class ObjectInflater implements Inflater<Object> {
        @Override
        public Object inflate(int ref) {
            return inflateObject(ref);
        }
    }

    private class DescriptorInflater implements Inflater<ChioneDescriptor> {
        @Override
        public ChioneDescriptor inflate(int ref) {
            return inflateDescriptor(ref);
        }
    }

    private class WrapperDeserializationContext implements DeserializationContext {

        private boolean hooked;
        private ChioneDescriptor descriptor;
        private int ref;

        WrapperDeserializationContext init(ChioneDescriptor descriptor, int ref) {
            hooked = false;
            this.descriptor = descriptor;
            this.ref = ref;
            return this;
        }

        WrapperDeserializationContext deinit() {
            if (!hooked) {
                throw new ChioneException("DeserializationContext.hookCreation hasn't been called during " + descriptor.getClassName() + " deserialization");
            }
            hooked = false;
            return this;
        }

        @Override
        public ChioneDataInput input() {
            return ChioneInputWrapper.this;
        }

        @Override
        public ChioneDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public <T> T hookCreation(T obj) {
            if (hooked) {
                throw new ChioneException();
            }
            hooked = true;
            objects[ref] = obj;
            return obj;
        }

    }

    private class ContextPool {

        private final Deque<WrapperDeserializationContext> pool = new ArrayDeque<>();

        WrapperDeserializationContext get(ChioneDescriptor descriptor, int ref) {
            WrapperDeserializationContext instance = getFromPool();
            if (instance == null) {
                instance = new WrapperDeserializationContext();
            }
            return instance.init(descriptor, ref);
        }

        void recycle(WrapperDeserializationContext context) {
            pool.push(context.deinit());
        }

        private WrapperDeserializationContext getFromPool() {
            return pool.poll();
        }

    }

}
