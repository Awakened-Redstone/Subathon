package com.awakenedredstone.subathon.config;

public enum Mode {
    NONE(false),
    JUMP(true),
    SUPER_JUMP(true),
    SPEED(true),
    SLOWNESS(true),
    HEALTH(true),
    SUPER_HEALTH(true),
    POTION_CHAOS(false);
    //TODO: Create system for picking random features
    //RANDOM(false), TODO: Create system for random mode effects
    //ENTROPY(false), TODO: Create system for random game behavior
    //CHAOS(false); TODO: Create system for random chaos

    private final boolean valueBased;

    Mode(boolean valueBased) {
        this.valueBased = valueBased;
    }

    public boolean isValueBased() {
        return valueBased;
    }
}
