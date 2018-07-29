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

import java.util.Arrays;

//TODO tests
public class ArrayMap<K, V> {

    private Object[] storage = new Object[8];
    private int doubleCount = 0;

    public V get(K key) {
        for (int i = 0; i < doubleCount; i += 2) {
            if (key.equals(storage[i])) {
                @SuppressWarnings("unchecked")
                V result = (V) storage[i + 1];
                return result;
            }
        }
        return null;
    }

    public void put(K key, V value) {
        if (doubleCount >= storage.length) {
            storage = Arrays.copyOf(storage, storage.length * 2);
        }
        storage[doubleCount++] = key;
        storage[doubleCount++] = value;
    }

    public int size() {
        return doubleCount / 2;
    }

    @SuppressWarnings("unchecked")
    public K keyAt(int index) {
        return (K) storage[index * 2];
    }

    @SuppressWarnings("unchecked")
    public V valueAt(int index) {
        return (V) storage[index * 2 + 1];
    }

}
