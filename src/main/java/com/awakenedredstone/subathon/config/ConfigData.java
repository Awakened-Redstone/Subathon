package com.awakenedredstone.subathon.config;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigData {

    public String mode = Mode.JUMP.name();
    public String messageMode = MessageMode.OVERLAY.name();
    public int fontScale = 1;
    public int effectIncrement = 1;
    public float effectMultiplier = 0.1f;
    public boolean runAtServerStart = true;

    //TODO: Support for multiple channels
    public String channelId = "";
    public String channelName = "";
    public String channelDisplayName = "";

    public boolean enableSubs = false;
    public short subsPerIncrement = 1;
    public boolean onePerGift = false;

    public boolean enableBits = false;
    public short bitModifier = 1;
    public short bitMin = 500;
    public boolean cumulativeBits = false;
    public boolean onePerCheer = false;
    public boolean cumulativeIgnoreMin = true;

    public Map<String, Short> subModifiers = Stream.of(new Object[][] {
        { "prime", (short) 1 },
        { "tier1", (short) 1 },
        { "tier2", (short) 1 },
        { "tier3", (short) 1 },
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (Short) data[1]));
}
