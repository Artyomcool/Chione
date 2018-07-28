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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SnowFlakeRegistry {

    private final ChioneDescriptor expectedDescriptor;
    private final ArrayMap<ChioneDescriptor, ReadStrategy> stepsCache = new ArrayMap<>();

    public SnowFlakeRegistry(ChioneDescriptor expectedDescriptor) {
        this.expectedDescriptor = expectedDescriptor;
    }

    ReadStrategy getStrategy(ChioneDescriptor descriptor) {
        ReadStrategy strategy = stepsCache.get(descriptor);
        if (strategy == null) {
            List<SnowFlakeField> expected = fieldsFromDescriptor(expectedDescriptor);
            List<SnowFlakeField> stored = fieldsFromDescriptor(descriptor);
            ReadOnlyArray<Step> steps = ReadOnlyArray.fromList(compareSortedSameTyped(expected, stored));
            strategy = ReadStrategy.fromSteps(steps);
            stepsCache.put(descriptor, strategy);
        }
        return strategy;
    }

    private List<SnowFlakeField> fieldsFromDescriptor(ChioneDescriptor descriptor) {
        return descriptor.subDescriptors().get(0).fields;
    }

    private List<Step> compareSortedSameTyped(List<SnowFlakeField> expected, List<SnowFlakeField> stored) {
        Iterator<SnowFlakeField> expectedIterator = expected.iterator();
        if (!expectedIterator.hasNext()) {
            return Collections.emptyList();
        }

        Iterator<SnowFlakeField> storedIterator = stored.iterator();
        if (!storedIterator.hasNext()) {
            List<Step> result = new ArrayList<>(expected.size());
            do {
                result.add(Step.nullStep(expectedIterator.next()));
            } while (expectedIterator.hasNext());
            return result;
        }

        List<Step> result = new ArrayList<>();
        SnowFlakeField expectedNext = expectedIterator.next();
        SnowFlakeField storedNext = storedIterator.next();

        while (true) {
            Step step = compare(expectedNext, storedNext);
            result.add(step);

            if (step.hasExpected()) {
                if (!expectedIterator.hasNext()) {
                    break;
                }
                expectedNext = expectedIterator.next();
            }
            if (step.hasStored()) {
                if (!storedIterator.hasNext()) {
                    while (expectedIterator.hasNext()) {
                        expectedIterator.next();
                        result.add(Step.nullStep(expectedNext));
                    }
                    break;
                }
                storedNext = storedIterator.next();
            }
        }

        return result;
    }

    private Step compare(SnowFlakeField expected, SnowFlakeField stored) {
        int compared =  expected.name().compareTo(stored.name());
        if (compared == 0) {
            return Step.readStep(expected, stored);
        }
        return compared < 0 ? Step.nullStep(expected) : Step.skipStep(stored);
    }
}
