/*
 * The MIT License
 *
 * Copyright (c) 2018 Artyom Drozdov (https://github.com/artyomcool)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
