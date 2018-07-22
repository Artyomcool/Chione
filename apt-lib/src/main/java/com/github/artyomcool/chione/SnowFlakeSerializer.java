package com.github.artyomcool.chione;

public abstract class SnowFlakeSerializer<T extends SnowFlake> implements ChioneSerializer<T> {

    private final ChioneDescriptor descriptor;
    private final SnowFlakeRegistry registry;

    protected SnowFlakeSerializer(ChioneDescriptor descriptor) {
        this.descriptor = descriptor;
        this.registry = new SnowFlakeRegistry(descriptor);
    }

    @Override
    public ChioneDescriptor describe(T obj) {
        return descriptor;
    }

    @Override
    public void writeContent(T obj, ChioneDataOutput dataOutput) {
        obj.write(dataOutput);
    }

    @Override
    public T deserialize(DeserializationContext context) {
        T snowFlake = instantiate();
        context.hookCreation(snowFlake);
        ReadStrategy strategy = registry.getStrategy(context.descriptor());
        strategy.read(context.input(), snowFlake);
        return snowFlake;
    }

    protected abstract T instantiate();
}
