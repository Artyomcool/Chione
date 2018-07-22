package com.github.artyomcool.chione;

public interface DataInput {

    int BYTE_SIZE = 1;
    int SHORT_SIZE = 2;
    int INT_SIZE = 4;
    int LONG_SIZE = 8;

    void seek(int offset);

    int pos();

    int size();

    byte readByte();

    short readShort();

    int readInt();

    long readLong();

    String readString();

}
