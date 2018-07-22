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
