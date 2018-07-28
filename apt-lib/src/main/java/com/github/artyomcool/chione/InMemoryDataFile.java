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

import java.util.Arrays;

public class InMemoryDataFile implements DataFile {

    private byte[] data = new byte[32];
    private int pos = 0;
    private int maxCount = 0;

    @Override
    public void seek(int offset) {
        pos = offset;
    }

    @Override
    public int pos() {
        return pos;
    }

    @Override
    public int size() {
        return maxCount;
    }

    @Override
    public byte readByte() {
        return data[pos++];
    }

    @Override
    public short readShort() {
        int b1 = data[pos++] & 0xff;
        int b2 = data[pos++] & 0xff;
        return (short) (b1 << 8 | b2);
    }

    @Override
    public int readInt() {
        int s1 = readShort() & 0xffff;
        int s2 = readShort() & 0xffff;
        return s1 << 16 | s2;
    }

    @Override
    public long readLong() {
        long l1 = readInt() & 0xffffffffL;
        long l2 = readInt() & 0xffffffffL;
        return l1 << 32 | l2;
    }

    @Override
    public String readString() {
        int size = readShort();
        char[] chars = new char[size];
        for (int i = 0; i < size; i++) {
            chars[i] = (char) readShort();
        }
        return new String(chars);
    }

    @Override
    public int write(byte b) {
        ensureSizeForByte();
        data[pos++] = b;
        if (pos > maxCount) {
            maxCount = pos;
        }
        return 1;
    }

    @Override
    public int write(short s) {
        write((byte) (s >>> 8));
        write((byte) (s & 0xff));
        return 2;
    }

    @Override
    public int write(int i) {
        write((short) (i >>> 16));
        write((short) (i & 0xffff));
        return 4;
    }

    @Override
    public int write(long l) {
        write((int) (l >>> 32));
        write((int) l);
        return 8;
    }

    @Override
    public int write(String s) {
        write((short) s.length());
        for (int i = 0; i < s.length(); i++) {
            write((short) s.charAt(i));
        }
        return s.length() * 2 + 2;
    }

    @Override
    public int write(byte[] data) {
        write(data.length);
        for (byte b : data) {
            write(b);
        }
        return data.length + 4;
    }

    private void ensureSizeForByte() {
        if (maxCount == data.length) {
            data = Arrays.copyOf(data, data.length * 3 / 2);
        }
    }

}
