package com.awakenedredstone.subathon.owo;

import io.wispforest.owo.config.ui.component.ConfigTextBox;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubathonTextBox extends ConfigTextBox {

    public ConfigTextBox configureForUuid() {
        this.setMaxLength(37);
        this.valueParser = s -> {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException nfe) {
                return "";
            }
        };

        Pattern pattern = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

        this.inputPredicate(s -> {
            Matcher matcher = pattern.matcher(s);
            return matcher.matches() || matcher.hitEnd() || s.isEmpty();
        });
        this.applyPredicate(s -> pattern.matcher(s).matches() || s.isEmpty());

        return this;
    }

    public ConfigTextBox configureForTwitchUsername() {
        this.setMaxLength(24);
        Pattern pattern = Pattern.compile("^[a-zA-Z\\d]\\w{0,24}$");
        this.valueParser = s -> pattern.matcher(s).matches() ? s : "";

        this.inputPredicate(s -> {
            Matcher matcher = pattern.matcher(s);
            return matcher.matches() || matcher.hitEnd() || s.isEmpty();
        });
        this.applyPredicate(s -> pattern.matcher(s).matches() || s.isEmpty());

        return this;
    }
}