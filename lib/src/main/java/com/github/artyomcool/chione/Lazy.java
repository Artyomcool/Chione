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

import static com.github.artyomcool.chione.Util.unsafeCast;

public class Lazy<T> {

    static final String CLASS_NAME = "SnowLazy";

    private static final ChioneDescriptor DESCRIPTOR = new ChioneDescriptor(CLASS_NAME);

    static final ChioneSerializer<Lazy<Object>> REGISTRY = new ChioneSerializer<Lazy<Object>>() {

        @Override
        public ChioneDescriptor describe(Lazy<Object> obj) {
            return DESCRIPTOR;
        }

        @Override
        public void writeContent(Lazy<Object> obj, ChioneDataOutput dataOutput) {
            dataOutput.writeReference(obj.get());
        }

        @Override
        public Lazy<Object> deserialize(DeserializationContext context) {
            ChioneDataInput input = context.input();
            return context.hookCreation(new Lazy<Object>(new LazyData(input, input.pos())));
        }
    };

    private Object lazy;

    public Lazy(T obj) {
        lazy = obj;
    }

    public T get() {
        if (lazy instanceof LazyData) {
            lazy = ((LazyData) lazy).inflate();
        }
        return unsafeCast(lazy);
    }

    public void set(T data) {
        lazy = data;
    }

    boolean isLoaded() {
        return !(lazy instanceof LazyData);
    }

    private static class LazyData {
        private final ChioneDataInput input;
        private final int pos;

        private LazyData(ChioneDataInput input, int pos) {
            this.input = input;
            this.pos = pos;
        }

        public Object inflate() {
            int oldPos = input.pos();
            input.seek(pos);
            try {
                return input.readReference();
            } finally {
                input.seek(oldPos);
            }
        }
    }

}
