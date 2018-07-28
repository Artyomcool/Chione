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
