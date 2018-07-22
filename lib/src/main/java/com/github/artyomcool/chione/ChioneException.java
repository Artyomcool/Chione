package com.github.artyomcool.chione;

public class ChioneException extends RuntimeException {

    public ChioneException() {
    }

    public ChioneException(String message) {
        super(message);
    }

    public ChioneException(Exception e) {
        super(e);
    }

}
