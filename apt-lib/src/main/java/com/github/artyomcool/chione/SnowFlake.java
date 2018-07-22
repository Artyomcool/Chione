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
