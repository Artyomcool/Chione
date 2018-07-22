package com.github.artyomcool.chione;

public interface ChioneDataInput extends DataInput {

    <T> T readReference();

}
