package com.awakenedredstone.subathon.twitch;

public enum Subscription {
    NONE("None", -1),
    PRIME("Prime", 0),
    TIER1("Tier 1", 1),
    TIER2("Tier 2", 2),
    TIER3("Tier 3", 3);

    private final String name;
    private final int value;

    Subscription(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
