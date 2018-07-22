package com.github.artyomcool.chione;

import java.util.Collections;
import java.util.Map;

public class Chione {

    private static final int TABLE_BLOCK_SIZE = 1024;
    private static final int SIGNATURE = 0x524f434b;

    private final ChioneSerializer<Object> serializer;

    public Chione() {
        this(Collections.<String, ChioneSerializer<?>>emptyMap());
    }

    public Chione(Map<String, ChioneSerializer<?>> serializers) {
        this(new SerializerRegistry(serializers));
    }

    public Chione(ChioneSerializer<Object> serializer) {
        this.serializer = serializer;
    }

    public void serialize(Object rock, DataOutput output) {
        Recollection<Object> recollection = new RecollectionRegistry();
        ChioneRegistry registry = new ChioneRegistry(recollection);
        ChioneDataOutput chioneOutput = new ChioneOutputWrapper(output, registry);

        chioneOutput.write(SIGNATURE);
        chioneOutput.writeReference(rock);

        int objectsOffset = poll(chioneOutput, registry);

        chioneOutput.write(objectsOffset);
        chioneOutput.write(SIGNATURE);
    }

    public <T> T deserialize(DataInput input, boolean lazy) {
        int size = input.size();

        checkSignature(input.readInt());
        int startPos = input.pos();

        input.seek(size - DataInput.INT_SIZE * 2);
        int objectsOffset = input.readInt();

        checkSignature(input.readInt());
        int[] objectsTable = readTable(input, objectsOffset);

        input.seek(startPos);

        ChioneInputWrapper wrapper = new ChioneInputWrapper(serializer, input, objectsTable);
        return wrapper.readReference();
    }

    private int poll(ChioneDataOutput output, ChioneRegistry registry) {
        int[] block = new int[TABLE_BLOCK_SIZE];
        int current = 0;
        int prevOffset = 0;

        while (true) {
            Object next = registry.poll();
            if (next == null) {
                break;
            }
            int offset = output.currentOffset();

            block[current++] = offset;

            ChioneDescriptor descriptor = serializer.describe(next);
            output.writeReference(descriptor);
            serializer.writeContent(next, output);

            if (current == TABLE_BLOCK_SIZE) {
                prevOffset = writeTable(output, block, current, prevOffset);
                current = 0;
            }
        }
        if (current != 0) {
            prevOffset = writeTable(output, block, current, prevOffset);
        }
        return prevOffset;
    }

    private int writeTable(ChioneDataOutput output, int[] block, int count, int prevOffset) {
        int start = output.currentOffset();
        output.write(prevOffset);
        output.write(count);
        for (int i = 0; i < count; i++) {
            output.write(block[i]);
        }

        return start;
    }

    private void checkSignature(int signature) {
        if (signature != SIGNATURE) {
            throw new ChioneException();
        }
    }

    private int[] readTable(DataInput input, int offset) {
        int size = tableSize(input, offset);
        int[] table = new int[size];

        int nextIndex = 0;

        int prevOffset = offset;

        do {
            input.seek(prevOffset);
            prevOffset = input.readInt();

            for (int i = input.readInt(); i > 0; i--) {
                table[nextIndex++] = input.readInt();
            }

        } while (prevOffset != 0);

        return table;
    }

    private int tableSize(DataInput input, int offset) {
        int prevOffset = offset;
        int size = 0;

        do {
            input.seek(prevOffset);
            prevOffset = input.readInt();
            size += input.readInt();
        } while (prevOffset != 0);

        return size;
    }
}
