package com.github.artyomcool.chione;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.artyomcool.chione.Util.unsafeCast;

public class SerializerRegistry implements ChioneSerializer<Object> {

    private final Map<String, ChioneSerializer<?>> serializers = new HashMap<>();

    public SerializerRegistry(Map<String, ChioneSerializer<?>> serializers) {
        @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
        final Class<? extends List> aClass = Arrays.asList().getClass();
        this.serializers.put(aClass.getName(), new ChioneSerializer<Object>() {
            @Override
            public ChioneDescriptor describe(Object obj) {
                return new ChioneDescriptor(aClass.getName());
            }

            @SuppressWarnings("ForLoopReplaceableByForEach")
            @Override
            public void writeContent(Object obj, ChioneDataOutput dataOutput) {
                List list = (List) obj;
                int size = list.size();
                dataOutput.write(size);
                for (int i = 0; i < size; i++) {
                    dataOutput.writeReference(list.get(i));
                }
            }

            @Override
            public Object deserialize(DeserializationContext context) {
                int size = context.input().readInt();
                Object[] result = new Object[size];
                for (int i = 0; i < size; i++) {
                    result[i] = context.input().readReference();
                }
                return context.hookCreation(Arrays.asList(result));
            }
        });

        this.serializers.put(Lazy.CLASS_NAME, Lazy.REGISTRY);

        this.serializers.putAll(serializers);
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
        return getSerializer(getName(object));
    }

    private String getName(Object object) {
        if (object instanceof Named) {
            return ((Named) object).chioneName();
        }
        return object.getClass().getName();
    }

    private <T> ChioneSerializer<T> getSerializer(ChioneDescriptor descriptor) {
        String className = descriptor.getClassName();
        return getSerializer(className);
    }

    private  <T> ChioneSerializer<T> getSerializer(String className) {
        if (className.equals(StringSerializer.CLASS_NAME)) {
            return unsafeCast(StringSerializer.INSTANCE);
        }

        if (className.equals(ChioneDescriptor.CLASS_NAME)) {
            return unsafeCast(ChioneDescriptor.DESERIALIZER);
        }

        if (className.startsWith("[")) {
            return unsafeCast(ArraySerializer.INSTANCE);
        }

        ChioneSerializer<?> chioneSerializer = serializers.get(className);
        if (chioneSerializer != null) {
            return unsafeCast(chioneSerializer);
        }

        throw new IllegalArgumentException("No serializer for class " + className);
    }

}
