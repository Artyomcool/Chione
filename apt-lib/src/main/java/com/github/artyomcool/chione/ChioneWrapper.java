package com.github.artyomcool.chione;

public interface ChioneWrapper<T> {
    void save(T root);
    T load();
}
