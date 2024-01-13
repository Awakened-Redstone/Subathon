package com.awakenedredstone.subathon.util;

import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Texts {
    private static final Map<String, String> EMPTY_STRING_MAP = Collections.emptyMap();
    private static final Map<String, Text> EMPTY_TEXT_MAP = Collections.emptyMap();

    public static String getTextString(Text text) {
        return text.getString();
    }

    public static Text prefixed(String prefixKey, Text key, Map<String, Text> placeholders) {
        Text prefix = of(Text.translatable(prefixKey));
        Text text = of(key, placeholders);

        return Text.empty().append(prefix).append(text);
    }

    public static Text prefixed(String prefixKey, Text key, Consumer<Map<String, Text>> builder) {
        Map<String, Text> placeholders = new HashMap<>();
        builder.accept(placeholders);

        return prefixed(prefixKey, key, placeholders);
    }

    public static Text prefixed(String prefixKey, Text key) {
        return prefixed(prefixKey, key, EMPTY_TEXT_MAP);
    }

    public static Text prefixed(String prefixKey, String key, Map<String, String> placeholders) {
        Text prefix = of(Text.translatable(prefixKey));
        Text text = of(key, placeholders);

        return Text.empty().append(prefix).append(text);
    }

    public static Text prefixed(String prefixKey, String key, Consumer<Map<String, String>> builder) {
        Map<String, String> placeholders = new HashMap<>();
        builder.accept(placeholders);

        return prefixed(prefixKey, key, placeholders);
    }

    public static Text prefixed(String prefixKey, String key) {
        return prefixed(prefixKey, key, EMPTY_STRING_MAP);
    }

    public static Text of(String key, Map<String, String> placeholders1) {
        Map<String, Text> placeholders = new HashMap<>();
        placeholders1.forEach((k, v) -> placeholders.put(k, Text.literal(v)));

        return of(Text.translatable(key), placeholders);
    }

    public static Text of(String key, Consumer<Map<String, String>> builder) {
        Map<String, String> placeholders1 = new HashMap<>();
        builder.accept(placeholders1);

        Map<String, Text> placeholders = new HashMap<>();
        placeholders1.forEach((k, v) -> placeholders.put(k, Text.literal(v)));

        return of(Text.translatable(key), placeholders);
    }

    public static Text of(String key) {
        return of(key, EMPTY_STRING_MAP);
    }

    public static Text of(Text key, Map<String, Text> placeholders) {
        Text text = DynamicPlaceholders.parseText(key, placeholders);
        return TextParserUtils.formatText(text.getString());
    }

    public static Text of(Text key, Consumer<Map<String, Text>> builder) {
        Map<String, Text> placeholders = new HashMap<>();
        builder.accept(placeholders);

        return of(key, placeholders);
    }

    public static Text of(Text key) {
        return of(key, EMPTY_TEXT_MAP);
    }
}
