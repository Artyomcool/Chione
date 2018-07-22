package com.github.artyomcool.chione;

public interface Interceptor<T> {

    T intercept(T obj);

}
