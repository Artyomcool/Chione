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

import java.lang.reflect.Array;

//TODO optimize by caching on the registry side specific instances
public class ArraySerializer implements ChioneSerializer<Object> {

    public static final ArraySerializer INSTANCE = new ArraySerializer();

    private ArraySerializer() {
    }

    @Override
    public ChioneDescriptor describe(Object obj) {
        return new ChioneDescriptor(obj.getClass().getName());
    }

    @Override
    public void writeContent(Object obj, ChioneDataOutput dataOutput) {
        switch (obj.getClass().getName().charAt(1)) {
            case 'Z':
                writeBooleanArray((boolean[]) obj, dataOutput);
                break;
            case 'B':
                writeByteArray((byte[]) obj, dataOutput);
                break;
            case 'S':
                writeShortArray((short[]) obj, dataOutput);
                break;
            case 'C':
                writeCharArray((char[]) obj, dataOutput);
                break;
            case 'I':
                writeIntArray((int[]) obj, dataOutput);
                break;
            case 'J':
                writeLongArray((long[]) obj, dataOutput);
                break;
            case 'F':
                writeFloatArray((float[]) obj, dataOutput);
                break;
            case 'D':
                writeDoubleArray((double[]) obj, dataOutput);
                break;
            case 'L':
            case '[':
                writeRefArray((Object[]) obj, dataOutput);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + obj);
        }
    }

    @Override
    public Object deserialize(DeserializationContext context) {
        ChioneDataInput input = context.input();
        String className = context.descriptor().getClassName();
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ChioneException(e);
        }
        Object array = context.hookCreation(Array.newInstance(clazz.getComponentType(), input.readInt()));
        switch (className.charAt(1)) {
            case 'Z':
                fillBooleanArray((boolean[]) array, input);
                break;
            case 'B':
                fillByteArray((byte[]) array, input);
                break;
            case 'S':
                fillShortArray((short[]) array, input);
                break;
            case 'C':
                fillCharArray((char[]) array, input);
                break;
            case 'I':
                fillIntArray((int[]) array, input);
                break;
            case 'J':
                fillLongArray((long[]) array, input);
                break;
            case 'F':
                fillFloatArray((float[]) array, input);
                break;
            case 'D':
                fillDoubleArray((double[]) array, input);
                break;
            case 'L':
            case '[':
                fillRefArray((Object[]) array, input);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type: " + className);
        }
        return array;
    }

    private void writeBooleanArray(boolean[] array, ChioneDataOutput output) {
        output.write(array.length);
        for (boolean i : array) {
            output.write((byte) (i ? 1 : 0));
        }
    }

    private void writeByteArray(byte[] array, ChioneDataOutput output) {
        output.write(array);
    }

    private void writeShortArray(short[] array, ChioneDataOutput output) {
        output.write(array.length);
        for (short i : array) {
            output.write(i);
        }
    }

    private void writeCharArray(char[] array, ChioneDataOutput output) {
        output.write(array.length);
        for (char i : array) {
            output.write((short) i);
        }
    }

    private void writeIntArray(int[] array, ChioneDataOutput output) {
        output.write(array.length);
        for (int i : array) {
            output.write(i);
        }
    }

    private void writeLongArray(long[] array, ChioneDataOutput output) {
        output.write(array.length);
        for (long i : array) {
            output.write(i);
        }
    }

    private void writeFloatArray(float[] array, ChioneDataOutput output) {
        output.write(array.length);
        for (float i : array) {
            output.write(Float.floatToRawIntBits(i));
        }
    }

    private void writeDoubleArray(double[] array, ChioneDataOutput output) {
        output.write(array.length);
        for (double i : array) {
            output.write(Double.doubleToRawLongBits(i));
        }
    }

    private void writeRefArray(Object[] array, ChioneDataOutput output) {
        output.write(array.length);
        for (Object i : array) {
            output.writeReference(i);
        }
    }

    private void fillBooleanArray(boolean[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = input.readByte() > 0;
        }
    }

    //TODO optimize
    private void fillByteArray(byte[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = input.readByte();
        }
    }

    private void fillShortArray(short[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = input.readShort();
        }
    }

    private void fillCharArray(char[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = (char) input.readShort();
        }
    }

    private void fillIntArray(int[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = input.readInt();
        }
    }

    private void fillLongArray(long[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = input.readLong();
        }
    }

    private void fillFloatArray(float[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = Float.intBitsToFloat(input.readInt());
        }
    }

    private void fillDoubleArray(double[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = Double.longBitsToDouble(input.readLong());
        }
    }

    private void fillRefArray(Object[] array, ChioneDataInput input) {
        for (int i = 0; i < array.length; i++) {
            array[i] = input.readReference();
        }
    }
}
