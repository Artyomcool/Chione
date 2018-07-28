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

import java.util.Map;

abstract class AbstractMapSerializer<T extends Map<Object, Object>> extends AbstractChioneSerializer<T> {
    AbstractMapSerializer(String className) {
        super(className);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public void writeContent(T map, ChioneDataOutput dataOutput) {
        int size = map.size();
        dataOutput.write(size);
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            dataOutput.writeReference(entry.getKey());
            dataOutput.writeReference(entry.getValue());
        }
    }

    @Override
    public T deserialize(DeserializationContext context) {
        ChioneDataInput input = context.input();
        int size = input.readInt();
        T result = context.hookCreation(create(size));
        for (int i = 0; i < size; i++) {
            Object key = input.readReference();
            Object value = input.readReference();
            result.put(key, value);
        }
        return result;
    }

    protected abstract T create(int size);

}
