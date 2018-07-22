package com.github.artyomcool.chione;

import java.util.*;

import static com.github.artyomcool.chione.Util.unsafeCast;

public class SerializerRegistry implements ChioneSerializer<Object> {

    private final Map<String, ChioneSerializer<?>> serializers = new HashMap<>();
    private final Map<Class<?>, ChioneSerializer<?>> aliases = new HashMap<>();

    public SerializerRegistry(Map<String, ChioneSerializer<?>> serializers) {
        register(arrayAsListClass(), new AbstractListSerializer("$Arrays.asList") {

            @Override
            public List<?> deserialize(DeserializationContext context) {
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

        register(ArrayList.class, new AbstractListSerializer("$ArrayList") {
            @Override
            protected List<Object> create(int size) {
                return new ArrayList<>(size);
            }
        });

        this.serializers.put(Lazy.CLASS_NAME, Lazy.REGISTRY);

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
        ChioneSerializer<?> serializer = aliases.get(object.getClass());
        if (serializer != null) {
            return unsafeCast(serializer);
        }
        return unsafeCast(getSerializer(getName(object)));
    }

    private String getName(Object object) {
        if (object instanceof Named) {
            return ((Named) object).chioneName();
        }
        return object.getClass().getName();
    }

    private <T> ChioneSerializer<T> getSerializer(ChioneDescriptor descriptor) {
        String className = descriptor.getClassName();
        return unsafeCast(getSerializer(className));
    }

    private ChioneSerializer<?> getSerializer(String className) {
        if (className.startsWith("[")) {
            return ArraySerializer.INSTANCE;
        }

        ChioneSerializer<?> chioneSerializer = serializers.get(className);
        if (chioneSerializer != null) {
            return chioneSerializer;
        }

        throw new IllegalArgumentException("No serializer for class " + className);
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static Class<?> arrayAsListClass() {
        return Arrays.asList().getClass();
    }

}
