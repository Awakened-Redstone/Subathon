package com.awakenedredstone.subathon.twitch;

public enum Subscription {
    PRIME("Prime"),
    TIER1("Tier 1"),
    TIER2("Tier 2"),
    TIER3("Tier 3");

    private final String name;

    Subscription(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
