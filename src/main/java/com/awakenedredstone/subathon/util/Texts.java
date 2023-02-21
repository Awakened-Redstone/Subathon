package com.awakenedredstone.subathon.util;

import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Texts {
    private static final Consumer<Map<String, String>> EMPTY_CONSUMER = a -> {/**/};

    public static Text prefixed(String prefixKey, String key, Consumer<Map<String, String>> builder) {
        String prefix = Language.getInstance().get(prefixKey);
        String text = prefix + translate(key, builder);
        return TextParserUtils.formatText(text);
    }

    public static Text prefixed(String prefixKey, String key) {
        return prefixed(prefixKey, key, EMPTY_CONSUMER);
    }

    public static Text of(String key, Map<String, String> map) {
        return of(key, m -> m.putAll(map));
    }

    public static Text of(String key, Consumer<Map<String, String>> builder) {
        return TextParserUtils.formatText(translate(key, builder));
    }

    public static Text of(String key) {
        return of(key, EMPTY_CONSUMER);
    }

    private static String translate(String key, Consumer<Map<String, String>> builder) {
        String text = Language.getInstance().get(key);
        Map<String, String> placeholders = new HashMap<>();
        builder.accept(placeholders);

        for (String k : placeholders.keySet()) {
            String v = placeholders.get(k);
            text = text.replaceAll(k, v);
        }

        return text;
    }
}
