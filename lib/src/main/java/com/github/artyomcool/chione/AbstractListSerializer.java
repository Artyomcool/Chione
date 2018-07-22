package com.github.artyomcool.chione;

import java.util.List;
import java.util.RandomAccess;

abstract class AbstractListSerializer extends AbstractChioneSerializer<List<?>> {
    AbstractListSerializer(String className) {
        super(className);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public void writeContent(List<?> list, ChioneDataOutput dataOutput) {
        int size = list.size();
        dataOutput.write(size);
        if (list instanceof RandomAccess) {
            for (int i = 0; i < size; i++) {
                dataOutput.writeReference(list.get(i));
            }
        } else {
            for (Object obj : list) {
                dataOutput.writeReference(obj);
            }
        }
    }

    @Override
    public List<?> deserialize(DeserializationContext context) {
        ChioneDataInput input = context.input();
        int size = input.readInt();
        List<Object> result = context.hookCreation(create(size));
        for (int i = 0; i < size; i++) {
            result.add(input.readReference());
        }
        return result;
    }

    protected abstract List<Object> create(int size);

}
