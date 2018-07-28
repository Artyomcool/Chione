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

import java.util.HashMap;
import java.util.Map;

import static com.github.artyomcool.chione.Util.unsafeCast;

public class RecollectionRegistry implements Recollection<Object> {

    private final Map<Class<?>, Recollection<?>> recollections = new HashMap<>();

    private final Recollection<Object> equalsRecollection = new MapRecollection<>(
            new HashMapInt<>(128, 0.75f)
    );
    private final Recollection<Object> identityRecollection = new MapRecollection<>(
            new IdentityHashMapInt<>(128, 0.75f)
    );

    {
        identityRecollection.remember(StringSerializer.DESCRIPTOR, new ReferenceAllocator() {
            @Override
            public int nextReference() {
                return StringSerializer.DESCRIPTOR_STATIC_REFERENCE;
            }
        });
    }

    @Override
    public int remember(Object obj, ReferenceAllocator referenceAllocator) {
        return getRecollection(obj).remember(obj, referenceAllocator);
    }

    private <T> Recollection<T> getRecollection(T obj) {
        Class<?> objClass = obj.getClass();
        Recollection<?> recollection = recollections.get(objClass);

        //noinspection Java8MapApi
        if (recollection == null) {
            recollection = getRecollectionForClass(objClass);
            recollections.put(objClass, recollection);
        }

        return unsafeCast(recollection);
    }

    private <T> Recollection<? super T> getRecollectionForClass(Class<T> clazz) {
        if (clazz == String.class) {
            return equalsRecollection;
        }
        return identityRecollection;
    }

}
