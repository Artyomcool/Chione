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

class ReadStrategy {

    private final ReadOnlyArray<Step> steps;
    private final boolean hasSkip;
    private final boolean hasNull;
    private final boolean hasTransform;

    static ReadStrategy fromSteps(ReadOnlyArray<Step> steps) {
        return new ReadStrategy(steps);
    }

    private ReadStrategy(ReadOnlyArray<Step> steps) {
        this.steps = steps;

        boolean hasSkip = false;
        boolean hasNull = false;
        boolean hasTransform = false;
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            if (!step.hasExpected()) {
                hasNull = true;
            }
            if (!step.hasStored()) {
                hasSkip = true;
            }
        }
        this.hasSkip = hasSkip;
        this.hasNull = hasNull;
        this.hasTransform = hasTransform;
    }

    void read(ChioneDataInput input, SnowFlake snowFlake) {
        if (hasSkip || hasNull) {
            if (hasTransform) {
                //TODO
                throw new UnsupportedOperationException();
            } else {
                snowFlake.readSimple(input, steps);
            }
        } else {
            snowFlake.readTrivial(input);
        }
    }

}
