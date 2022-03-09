package com.awakenedredstone.subathon.config.cloth;

import com.awakenedredstone.subathon.Effect;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.cloth.options.ButtonFieldBuilder;
import com.awakenedredstone.subathon.config.cloth.options.PasteFieldBuilder;
import com.awakenedredstone.subathon.config.cloth.options.ShortFieldBuilder;
import com.awakenedredstone.subathon.twitch.Bot;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class ClothConfig {

    public Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create().setTitle(new TranslatableText("title.subathon.config"));
        if (parent != null) {
            builder.setParentScreen(parent);
        }
        ConfigCategory general = builder.getOrCreateCategory(new TranslatableText("category.subathon.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startDropdownMenu(new TranslatableText("option.subathon.mode"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(Effect.valueOf(Subathon.getConfigData().effect), FUNCTION, (effect) -> new LiteralText(effect.toString())),
                        DropdownMenuBuilder.CellCreatorBuilder.of())
                .setDefaultValue(Effect.valueOf("JUMP"))
                .setTooltip(new TranslatableText("option.subathon.mode.description"))
                .setSelections(Arrays.stream(Effect.values()).toList())
                .setSaveConsumer(mode -> Subathon.getConfigData().effect = mode.toString())
                .build());

        general.addEntry(entryBuilder.startFloatField(new TranslatableText("option.subathon.effect_amplifier"), Subathon.getConfigData().effectAmplifier)
                .setDefaultValue(0.1f)
                .setTooltip(new TranslatableText("option.subathon.effect_amplifier.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().effectAmplifier = newValue)
                .build());

        general.addEntry(new ShortFieldBuilder(new TranslatableText("option.subathon.sub_amplifier"), Subathon.getConfigData().subModifier)
                .setDefaultValue((short) 1)
                .setTooltip(new TranslatableText("option.subathon.sub_amplifier.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().subModifier = newValue)
                .build());

        general.addEntry(new PasteFieldBuilder(new TranslatableText("option.subathon.auth_data"), "{}")
                .setDefaultValue("{}")
                .setType("JSON")
                .setTooltip(new TranslatableText("option.subathon.auth_data.description"))
                .setSaveConsumer(Subathon.auth::fromString)
                .build());

        general.addEntry(new ButtonFieldBuilder(new TranslatableText("option.subathon.auth"))
                .setTooltip(new TranslatableText("option.subathon.auth.description"))
                .setPressAction((button) -> Util.getOperatingSystem().open(Bot.getAuthenticationUrl(List.of("channel:read:subscriptions"), null)))
                .build());

        builder.setSavingRunnable(this::save);
        return builder.build();
    }

    private void save() {
        Subathon.config.save();
        Subathon.auth.save();
    }

    public static final Function<String, Effect> FUNCTION = (str) -> {
        try {
            return Effect.valueOf(str);
        } catch (Exception var2) {
            return Effect.NONE;
        }
    };
}
