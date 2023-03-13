// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.awakenedredstone.subathon.command.argument;

import com.awakenedredstone.subathon.twitch.Twitch;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class TwitchUsernameArgumentType implements ArgumentType<String> {
    private static final DynamicCommandExceptionType INVALID_USERNAME_EXCEPTION = new DynamicCommandExceptionType((value) -> Text.translatable("argument.username.invalid", value));

    public static TwitchUsernameArgumentType create() {
        return new TwitchUsernameArgumentType();
    }

    public static String getUsername(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        String string = reader.readString();
        if (Pattern.compile("^[a-zA-Z\\d]\\w{0,24}$").matcher(string).matches()) {
            return string;
        } else {
            throw INVALID_USERNAME_EXCEPTION.create(string);
        }
    }

    @Override
    public String toString() {
        return "string()";
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("awakenedredstone", "CaptainSparklez");
    }
}
