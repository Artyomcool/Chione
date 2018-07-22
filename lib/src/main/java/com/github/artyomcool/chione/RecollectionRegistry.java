package com.github.artyomcool.chione;

import java.util.HashMap;
import java.util.Map;

import static com.github.artyomcool.chione.Util.unsafeCast;

public class RecollectionRegistry implements Recollection<Object> {

    private final Map<Class<?>, Recollection<?>> recollections = new HashMap<>();

    private final Recollection<Object> equalsRecollection = new MapRecollection<>(
            new HashMapInt<>(128, 0.75f)
    );
    private final Recollection<Object> identityRecollection = new MapRecollection<>(
            new IdentityHashMapInt<>(128, 0.75f)
    );

    {
        identityRecollection.remember(StringSerializer.DESCRIPTOR, new ReferenceAllocator() {
            @Override
            public int nextReference() {
                return StringSerializer.DESCRIPTOR_STATIC_REFERENCE;
            }
        });
    }

    @Override
    public int remember(Object obj, ReferenceAllocator referenceAllocator) {
        return getRecollection(obj).remember(obj, referenceAllocator);
    }

    private <T> Recollection<T> getRecollection(T obj) {
        Class<?> objClass = obj.getClass();
        Recollection<?> recollection = recollections.get(objClass);

        //noinspection Java8MapApi
        if (recollection == null) {
            recollection = getRecollectionForClass(objClass);
            recollections.put(objClass, recollection);
        }

        return unsafeCast(recollection);
    }

    private <T> Recollection<? super T> getRecollectionForClass(Class<T> clazz) {
        if (clazz == String.class) {
            return equalsRecollection;
        }
        return identityRecollection;
    }

}
