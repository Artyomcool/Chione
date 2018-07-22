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
