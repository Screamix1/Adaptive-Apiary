package com.screamix.adaptiveapiary.item;

public enum BeeType {
    DRONE("drone"),
    PRINCESS("princess"),
    QUEEN("queen");

    private final String key;

    BeeType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
