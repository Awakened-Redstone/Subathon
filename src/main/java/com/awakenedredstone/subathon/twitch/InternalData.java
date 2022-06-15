package com.awakenedredstone.subathon.twitch;

import net.minecraft.util.Identifier;

import java.util.TreeMap;

public class InternalData {
    public TreeMap<Identifier, Double> nextValues;
    public int bits;
    public int subs;

    public InternalData(int bits, int subs, TreeMap<Identifier, Double> nextValues) {
        this.bits = bits;
        this.subs = subs;
        this.nextValues = nextValues;
    }
}
