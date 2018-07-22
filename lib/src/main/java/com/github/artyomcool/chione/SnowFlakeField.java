package com.github.artyomcool.chione;

public class SnowFlakeField {

    private final String name;
    private final String type;

    public SnowFlakeField(String name, String type) {
        this.name = name;
        this.type = type;
    }

    final String name() {
        return name;
    }

    final String type() {
        return type;
    }

}
