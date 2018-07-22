package com.github.artyomcool.chione;

public class StringSerializer implements ChioneSerializer<String> {

    public static final String CLASS_NAME = "java.lang.String";
    public static final ChioneDescriptor DESCRIPTOR = new ChioneDescriptor(CLASS_NAME);
    public static final int DESCRIPTOR_STATIC_REFERENCE = Integer.MAX_VALUE;
    public static final StringSerializer INSTANCE = new StringSerializer();

    private StringSerializer() {
    }

    @Override
    public ChioneDescriptor describe(String obj) {
        return DESCRIPTOR;
    }

    @Override
    public void writeContent(String obj, ChioneDataOutput dataOutput) {
        dataOutput.write(obj);
    }

    @Override
    public String deserialize(DeserializationContext context) {
        ChioneDataInput input = context.input();
        return context.hookCreation(input.readString());
    }
}
