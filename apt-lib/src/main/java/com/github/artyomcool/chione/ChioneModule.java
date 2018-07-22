package com.github.artyomcool.chione;

public interface ChioneModule<T, F> {

    F factory();

    ChioneWrapper<T> chione();

}
