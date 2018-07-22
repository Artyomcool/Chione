package com.github.artyomcool.chione;

import javax.annotation.Nullable;

public interface ChioneDataOutput extends DataOutput {

    int writeReference(@Nullable Object obj);

    int currentOffset();

}
