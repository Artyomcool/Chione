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
