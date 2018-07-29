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

import static com.github.artyomcool.chione.Util.unsafeCast;

public class SerializerRegistry implements ChioneSerializer<Object> {

    private final Map<String, ChioneSerializer<?>> serializers = new HashMap<>();
    private final Map<Class<?>, ChioneSerializer<?>> aliases = new HashMap<>();
    private final ArrayMap<Class<?>, ChioneSerializer<?>> hierarchy = new ArrayMap<>();

    public SerializerRegistry(Map<String, ChioneSerializer<?>> serializers) {
        register(arrayAsListClass(), new AbstractCollectionSerializer<List<Object>>("$Arrays.asList") {

            @Override
            public List<Object> deserialize(DeserializationContext context) {
                int size = context.input().readInt();
                Object[] result = new Object[size];
                for (int i = 0; i < size; i++) {
                    result[i] = context.input().readReference();
                }
                return context.hookCreation(Arrays.asList(result));
            }

            @Override
            protected List<Object> create(int size) {
                throw new UnsupportedOperationException();
            }
        });

        register(Boolean.class, new AbstractWrapperSerializer<Boolean>("$Boolean") {
            @Override
            public void writeContent(Boolean obj, ChioneDataOutput dataOutput) {
                dataOutput.write(obj ? (byte) 1 : (byte) 0);
            }

            @Override
            Boolean read(ChioneDataInput input) {
                return input.readInt() > 0;
            }
        });

        register(Byte.class, new AbstractWrapperSerializer<Byte>("$Byte") {
            @Override
            public void writeContent(Byte obj, ChioneDataOutput dataOutput) {
                dataOutput.write(obj);
            }

            @Override
            Byte read(ChioneDataInput input) {
                return input.readByte();
            }
        });

        register(Short.class, new AbstractWrapperSerializer<Short>("$Short") {
            @Override
            public void writeContent(Short obj, ChioneDataOutput dataOutput) {
                dataOutput.write(obj);
            }

            @Override
            Short read(ChioneDataInput input) {
                return input.readShort();
            }
        });

        register(Character.class, new AbstractWrapperSerializer<Character>("$Character") {
            @Override
            public void writeContent(Character obj, ChioneDataOutput dataOutput) {
                dataOutput.write((short) obj.charValue());
            }

            @Override
            Character read(ChioneDataInput input) {
                return (char) input.readShort();
            }
        });

        register(Integer.class, new AbstractWrapperSerializer<Integer>("$Integer") {
            @Override
            public void writeContent(Integer obj, ChioneDataOutput dataOutput) {
                dataOutput.write(obj);
            }

            @Override
            Integer read(ChioneDataInput input) {
                return input.readInt();
            }
        });

        register(Long.class, new AbstractWrapperSerializer<Long>("$Long") {
            @Override
            public void writeContent(Long obj, ChioneDataOutput dataOutput) {
                dataOutput.write(obj);
            }

            @Override
            Long read(ChioneDataInput input) {
                return input.readLong();
            }
        });

        register(Float.class, new AbstractWrapperSerializer<Float>("$Float") {
            @Override
            public void writeContent(Float obj, ChioneDataOutput dataOutput) {
                dataOutput.write(Float.floatToRawIntBits(obj));
            }

            @Override
            Float read(ChioneDataInput input) {
                return Float.intBitsToFloat(input.readInt());
            }
        });

        register(Double.class, new AbstractWrapperSerializer<Double>("$Double") {
            @Override
            public void writeContent(Double obj, ChioneDataOutput dataOutput) {
                dataOutput.write(Double.doubleToRawLongBits(obj));
            }

            @Override
            Double read(ChioneDataInput input) {
                return Double.longBitsToDouble(input.readLong());
            }
        });

        register(ArrayList.class, new AbstractCollectionSerializer<ArrayList<Object>>("$ArrayList") {
            @Override
            protected ArrayList<Object> create(int size) {
                return new ArrayList<>(size);
            }
        });

        register(LinkedList.class, new AbstractCollectionSerializer<LinkedList<Object>>("$LinkedList") {
            @Override
            protected LinkedList<Object> create(int size) {
                return new LinkedList<>();
            }
        });

        register(HashSet.class, new AbstractCollectionSerializer<HashSet<Object>>("$HashSet") {
            @Override
            protected HashSet<Object> create(int size) {
                return new HashSet<>();
            }
        });

        register(TreeSet.class, new AbstractCollectionSerializer<TreeSet<Object>>("$TreeSet") {
            @Override
            protected TreeSet<Object> create(int size) {
                return new TreeSet<>();
            }
        });

        register(LinkedHashSet.class, new AbstractCollectionSerializer<LinkedHashSet<Object>>("$LinkedHashSet") {
            @Override
            protected LinkedHashSet<Object> create(int size) {
                return new LinkedHashSet<>();
            }
        });

        register(ArrayDeque.class, new AbstractCollectionSerializer<ArrayDeque<Object>>("$ArrayDeque") {
            @Override
            protected ArrayDeque<Object> create(int size) {
                return new ArrayDeque<>(size);
            }
        });

        register(HashMap.class, new AbstractMapSerializer<HashMap<Object, Object>>("$HashMap") {
            @Override
            protected HashMap<Object, Object> create(int size) {
                return new HashMap<>();
            }
        });

        register(LinkedHashMap.class, new AbstractMapSerializer<LinkedHashMap<Object, Object>>("$LinkedHashMap") {
            @Override
            protected LinkedHashMap<Object, Object> create(int size) {
                return new LinkedHashMap<>();
            }
        });

        register(Lazy.class, Lazy.CLASS_NAME, Lazy.REGISTRY);

        registerHierarchy(List.class, this.aliases.get(ArrayList.class));
        registerHierarchy(Set.class, this.aliases.get(HashSet.class));
        registerHierarchy(Map.class, this.aliases.get(HashMap.class));
        registerHierarchy(Queue.class, this.aliases.get(ArrayDeque.class));

        this.serializers.putAll(serializers);

        register(String.class, StringSerializer.CLASS_NAME, StringSerializer.INSTANCE);
        register(ChioneDescriptor.class, ChioneDescriptor.CLASS_NAME, ChioneDescriptor.DESERIALIZER);
    }

    private void register(Class<?> clazz, AbstractChioneSerializer<?> serializer) {
        register(clazz, serializer.getClassName(), serializer);
    }

    private void register(Class<?> clazz, String className, ChioneSerializer<?> serializer) {
        serializers.put(className, serializer);
        aliases.put(clazz, serializer);
    }

    private void registerHierarchy(Class<?> clazz, ChioneSerializer<?> serializer) {
        hierarchy.put(clazz, serializer);
    }

    @Override
    public ChioneDescriptor describe(Object obj) {
        return getSerializer(obj).describe(obj);
    }

    @Override
    public void writeContent(Object obj, ChioneDataOutput dataOutput) {
        getSerializer(obj).writeContent(obj, dataOutput);
    }

    @Override
    public Object deserialize(DeserializationContext context) {
        return getSerializer(context.descriptor()).deserialize(context);
    }

    private <T> ChioneSerializer<T> getSerializer(Object object) {
        Class<?> objectClass = object.getClass();
        ChioneSerializer<?> serializer = aliases.get(objectClass);
        if (serializer != null) {
            return unsafeCast(serializer);
        }
        serializer = getSerializer(getName(object));
        if (serializer != null) {
            return unsafeCast(serializer);
        }
        for (int i = 0; i < hierarchy.size(); i++) {
            Class<?> clazz = hierarchy.keyAt(i);
            if (clazz.isAssignableFrom(objectClass)) {
                return unsafeCast(hierarchy.valueAt(i));
            }
        }
        throw new IllegalArgumentException("No serializer for object " + object);
    }

    private String getName(Object object) {
        if (object instanceof Named) {
            return ((Named) object).chioneName();
        }
        return object.getClass().getName();
    }

    private <T> ChioneSerializer<T> getSerializer(ChioneDescriptor descriptor) {
        String className = descriptor.getClassName();
        ChioneSerializer<?> serializer = getSerializer(className);
        if (serializer == null) {
            throw new ChioneException("No serializer for class " + className);
        }
        return unsafeCast(serializer);
    }

    private ChioneSerializer<?> getSerializer(String className) {
        if (className.startsWith("[")) {
            return ArraySerializer.INSTANCE;
        }

        ChioneSerializer<?> chioneSerializer = serializers.get(className);
        if (chioneSerializer != null) {
            return chioneSerializer;
        }

        return null;
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static Class<?> arrayAsListClass() {
        return Arrays.asList().getClass();
    }

}
