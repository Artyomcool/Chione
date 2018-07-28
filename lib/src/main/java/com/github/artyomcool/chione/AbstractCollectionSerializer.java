package com.github.artyomcool.chione;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

abstract class AbstractCollectionSerializer<T extends Collection<Object>> extends AbstractChioneSerializer<T> {
    AbstractCollectionSerializer(String className) {
        super(className);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public void writeContent(T collection, ChioneDataOutput dataOutput) {
        int size = collection.size();
        dataOutput.write(size);
        if (collection instanceof RandomAccess && collection instanceof List) {
            List<?> list = (List<?>) collection;
            for (int i = 0; i < size; i++) {
                dataOutput.writeReference(list.get(i));
            }
        } else {
            for (Object obj : collection) {
                dataOutput.writeReference(obj);
            }
        }
    }

    @Override
    public T deserialize(DeserializationContext context) {
        ChioneDataInput input = context.input();
        int size = input.readInt();
        T result = context.hookCreation(create(size));
        for (int i = 0; i < size; i++) {
            result.add(input.readReference());
        }
        return result;
    }

    protected abstract T create(int size);

}
