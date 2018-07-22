package com.github.artyomcool.chione;

public interface DataOutput {

    int write(byte b);

    int write(short s);

    int write(int i);

    int write(long l);

    int write(String s);

    int write(byte[] data);

}
