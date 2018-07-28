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

public class StringSerializer implements ChioneSerializer<String> {

    public static final String CLASS_NAME = "$String";
    public static final ChioneDescriptor DESCRIPTOR = new ChioneDescriptor(CLASS_NAME);
    public static final int DESCRIPTOR_STATIC_REFERENCE = Integer.MAX_VALUE;
    public static final StringSerializer INSTANCE = new StringSerializer();

    private StringSerializer() {
    }

    @Override
    public ChioneDescriptor describe(String obj) {
        return DESCRIPTOR;
    }

    @Override
    public void writeContent(String obj, ChioneDataOutput dataOutput) {
        dataOutput.write(obj);
    }

    @Override
    public String deserialize(DeserializationContext context) {
        ChioneDataInput input = context.input();
        return context.hookCreation(input.readString());
    }
}
