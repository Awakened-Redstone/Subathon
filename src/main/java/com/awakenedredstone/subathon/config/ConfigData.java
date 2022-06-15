package com.awakenedredstone.subathon.config;

import com.awakenedredstone.subathon.twitch.Subscription;
import com.awakenedredstone.subathon.util.MapBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigData {
    //General
    public boolean runAtServerStart = true;
    public List<String> channels = new ArrayList<>();
    public int resetTimer = 0;
    public int updateTimer = 0;

    //CubeController
    public Map<String, Double> scales = new HashMap<>();
    public Map<String, Boolean> invoke = new HashMap<>();

    //Client
    public boolean showEventsInChat = true;
    public boolean showUpdateTimer = false;
    public boolean showToasts = true;
    public int minBitsForToast = 1000;
    public Subscription minSubTierForToast = Subscription.TIER2;
    public Map<Subscription, Byte> minSubsGiftedForToast = new MapBuilder<Subscription, Byte>()
            .put(Subscription.TIER1, (byte) 10)
            .put(Subscription.TIER2, (byte) 3)
            .put(Subscription.TIER3, (byte) 1)
            .build();

    //Bits
    public boolean enableBits = false;
    public short bitModifier = 1;
    public short bitMin = 500;
    public boolean cumulativeBits = false;
    public boolean onePerCheer = false;
    public boolean cumulativeIgnoreMin = true;

    //Subs
    public boolean enableSubs = true;
    public short subsPerIncrement = 1;
    public boolean onePerGift = false;
    public Map<Subscription, Short> subModifiers = new MapBuilder<Subscription, Short>()
            .put(Subscription.PRIME, (short) 1)
            .put(Subscription.TIER1, (short) 1)
            .put(Subscription.TIER2, (short) 1)
            .put(Subscription.TIER3, (short) 1)
            .build();
}
