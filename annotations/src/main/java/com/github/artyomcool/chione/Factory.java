package com.github.artyomcool.chione;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SuppressWarnings("WeakerAccess")
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME) //TODO will it be useful for some runtime behaviour?
public @interface Factory {
    Class<?> root();
}
