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
