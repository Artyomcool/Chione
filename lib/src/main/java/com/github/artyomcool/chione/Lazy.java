package com.github.artyomcool.chione;

import static com.github.artyomcool.chione.Util.unsafeCast;

public class Lazy<T> {

    static final String CLASS_NAME = "SnowLazy";

    private static final ChioneDescriptor DESCRIPTOR = new ChioneDescriptor(CLASS_NAME);

    static final ChioneSerializer<Lazy<Object>> REGISTRY = new ChioneSerializer<Lazy<Object>>() {

        @Override
        public ChioneDescriptor describe(Lazy<Object> obj) {
            return DESCRIPTOR;
        }

        @Override
        public void writeContent(Lazy<Object> obj, ChioneDataOutput dataOutput) {
            dataOutput.writeReference(obj.get());
        }

        @Override
        public Lazy<Object> deserialize(DeserializationContext context) {
            ChioneDataInput input = context.input();
            return new Lazy<Object>(new LazyData(input, input.pos()));
        }
    };

    private Object lazy;

    public Lazy(T obj) {
        lazy = obj;
    }

    public T get() {
        if (lazy instanceof LazyData) {
            lazy = ((LazyData) lazy).inflate();
        }
        return unsafeCast(lazy);
    }

    private static class LazyData {
        private final ChioneDataInput input;
        private final int pos;

        private LazyData(ChioneDataInput input, int pos) {
            this.input = input;
            this.pos = pos;
        }

        public Object inflate() {
            int oldPos = input.pos();
            input.seek(pos);
            try {
                return input.readReference();
            } finally {
                input.seek(oldPos);
            }
        }
    }

}
