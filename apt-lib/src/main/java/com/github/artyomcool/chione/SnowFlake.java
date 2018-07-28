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

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SnowFlake implements Named {

    protected abstract void write(ChioneDataOutput output);

    protected abstract void readTrivial(ChioneDataInput input);

    protected abstract void readSimple(ChioneDataInput input, ReadOnlyArray<Step> steps);

    protected final boolean hasCurrentField(ReadOnlyArray<Step> steps, int current) {
        return steps.size() > current && steps.get(current).hasStored();
    }

    protected final int skip(ChioneDataInput input, ReadOnlyArray<Step> steps, int current) {
        int size = steps.size();
        for (int i = current + 1; i < size; i++) {
            Step step = steps.get(i);
            if (step.hasExpected()) {
                return i;
            }
            if (step.hasStored()) {
                input.seek(input.pos() + step.storedSize());
            }
        }
        return size;
    }

}
