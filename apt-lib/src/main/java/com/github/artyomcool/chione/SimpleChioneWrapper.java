package com.github.artyomcool.chione;

public class SimpleChioneWrapper<T> implements ChioneWrapper<T> {

    private final Chione chione;
    private final DataFile file;

    public SimpleChioneWrapper(Chione chione, DataFile file) {
        this.chione = chione;
        this.file = file;
    }

    @Override
    public void save(T root) {
        file.seek(0);
        chione.serialize(root, file);
    }

    @Override
    public T load() {
        file.seek(0);
        return chione.deserialize(file, false);
    }
}
