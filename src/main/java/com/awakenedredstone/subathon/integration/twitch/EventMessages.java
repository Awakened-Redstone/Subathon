package com.awakenedredstone.subathon.integration.twitch;

public enum EventMessages {
    QUICK_SUB_TIER1("text.subathon.quick_notification.sub.tier1"),
    QUICK_SUB_TIER2("text.subathon.quick_notification.sub.tier2"),
    QUICK_SUB_TIER3("text.subathon.quick_notification.sub.tier3"),
    QUICK_GIFT_TIER1("text.subathon.quick_notification.gift.tier1"),
    QUICK_GIFT_TIER2("text.subathon.quick_notification.gift.tier2"),
    QUICK_GIFT_TIER3("text.subathon.quick_notification.gift.tier3"),
    QUICK_CHEER("text.subathon.quick_notification.cheer"),
    QUICK_ERROR("text.subathon.quick_notification.error"),

    SUB("text.subathon.notification.sub"),
    RESUB("text.subathon.notification.resub"),
    RESUB_MULTIMONTH("text.subathon.notification.resub_multimonth"),
    GIFT("text.subathon.notification.gift"),
    CHEER("text.subathon.notification.cheer"),
    ERROR("text.subathon.notification.error"),
    ;

    public final String translation;

    EventMessages(String translation) {
        this.translation = translation;
    }
}
