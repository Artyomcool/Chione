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

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class Step {
    @Nullable
    private final SnowFlakeField expected;
    @Nullable
    private final SnowFlakeField stored;

    static Step nullStep(SnowFlakeField expected) {
        return new Step(expected, null);
    }

    static Step skipStep(SnowFlakeField stored) {
        return new Step(null, stored);
    }

    static Step readStep(SnowFlakeField expected, SnowFlakeField stored) {
        return new Step(expected, stored);
    }

    private Step(@Nullable SnowFlakeField expected, @Nullable SnowFlakeField stored) {
        this.expected = expected;
        this.stored = stored;
    }

    public boolean hasExpected() {
        return expected != null;
    }

    public boolean hasStored() {
        return stored != null;
    }

    public int storedSize() {
        if (stored == null) {
            return 0;
        }
        switch (stored.type()) {
            case "byte":
                return 1;
            case "char":
            case "short":
                return 2;
            case "int":
            case "float":
            default:
                return 4;
            case "double":
            case "long":
                return 8;
        }
    }
}
