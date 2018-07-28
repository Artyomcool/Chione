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

import java.util.*;

public class ChioneDescriptor implements Named {

    static final String CLASS_NAME = "$ChioneDescriptor";

    private static final int CURRENT_VERSION = 1;

    private static final ChioneDescriptor DESCRIPTOR_DESCRIPTOR = new ChioneDescriptor(
            CLASS_NAME,
            CURRENT_VERSION
    );

    static final ChioneSerializer<ChioneDescriptor> DESERIALIZER =
            new ChioneSerializer<ChioneDescriptor>() {
                @Override
                public ChioneDescriptor describe(ChioneDescriptor obj) {
                    return DESCRIPTOR_DESCRIPTOR;
                }

                @Override
                public void writeContent(ChioneDescriptor obj, ChioneDataOutput dataOutput) {
                    obj.write(dataOutput);
                }

                @Override
                public ChioneDescriptor deserialize(DeserializationContext context) {
                    throw new UnsupportedOperationException();
                }
            };

    private int version;
    private List<SubDescriptor> subDescriptors;

    public ChioneDescriptor() {
    }

    public ChioneDescriptor(String className) {
        this(className, 1);
    }

    public ChioneDescriptor(String className, int version) {
        this(className, version, Collections.<SnowFlakeField>emptyList());
    }

    public ChioneDescriptor(String className, int version, List<SnowFlakeField> fields) {
        this.version = version;
        this.subDescriptors = Collections.singletonList(new SubDescriptor(className, fields));
    }

    public ChioneDescriptor(int version, List<SubDescriptor> subDescriptors) {
        this.version = version;
        this.subDescriptors = subDescriptors;
    }

    String getClassName() {
        return subDescriptors.get(0).className;
    }

    public List<SubDescriptor> subDescriptors() {
        return subDescriptors;
    }

    public int version() {
        return version;
    }

    void write(ChioneDataOutput output) {
        output.write(version);
        output.write(subDescriptors.size());
        for (SubDescriptor subDescriptor : subDescriptors) {
            writeClassName(output, subDescriptor.className);
            output.write(subDescriptor.fields.size());
            for (SnowFlakeField field : subDescriptor.fields) {
                output.writeReference(field.name());
                output.writeReference(field.type());
            }
        }
    }

    void read(ChioneDataInput input, ChioneDescriptor descriptor) {
        if (descriptor != this) {   //reading DescriptorDescriptor itself
            if (descriptor.version > CURRENT_VERSION) {
                throw new ChioneException();
            }
        }
        int version = input.readInt();
        int subDescriptorsCount = input.readInt();

        List<SubDescriptor> subDescriptors = new ArrayList<>(subDescriptorsCount);
        for (int i = 0; i < subDescriptorsCount; i++) {
            String className = readClassName(input);
            int fieldsCount = input.readInt();
            List<SnowFlakeField> fields = new ArrayList<>(fieldsCount);
            for (int j = 0; j < fieldsCount; j++) {
                String name = input.readReference();
                String type = input.readReference();
                fields.add(new SnowFlakeField(name, type));
            }
            subDescriptors.add(new SubDescriptor(className, fields));
        }
        this.version = version;
        this.subDescriptors = subDescriptors;
    }

    private void writeClassName(ChioneDataOutput output, String className) {
        int lastPos = className.lastIndexOf('.');
        if (lastPos == -1) {
            output.writeReference(null);
            output.writeReference(className);
        } else {
            String packageName = className.substring(0, lastPos);
            output.writeReference(packageName);
            output.writeReference(className.substring(lastPos + 1));
        }
    }

    private String readClassName(ChioneDataInput input) {
        String packageName = input.readReference();
        String className = input.readReference();
        if (packageName == null) {
            return className;
        }

        char[] chars = new char[packageName.length() + className.length() + 1];
        packageName.getChars(0, packageName.length(), chars, 0);
        chars[packageName.length()] = '.';
        className.getChars(0, className.length(), chars, packageName.length() + 1);
        return new String(chars);
    }

    public ChioneDescriptor withNewClassName(String name) {
        SubDescriptor old = subDescriptors.get(0);

        List<SubDescriptor> changed = new ArrayList<>(subDescriptors.size());
        changed.add(new SubDescriptor(old.className, old.fields));
        changed.addAll(subDescriptors.subList(1, subDescriptors.size()));

        return new ChioneDescriptor(version, changed);
    }

    @Override
    public String chioneName() {
        return CLASS_NAME;
    }

    public static class SubDescriptor {
        final String className;
        final List<SnowFlakeField> fields;

        SubDescriptor(String className, List<SnowFlakeField> fields) {
            this.className = className;
            this.fields = fields;
        }

        public String getClassName() {
            return className;
        }

        public List<SnowFlakeField> getFields() {
            return fields;
        }
    }

}
