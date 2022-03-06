package com.awakenedredstone.subathon.config.cloth;

import com.awakenedredstone.subathon.Effect;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.cloth.options.PasteFieldBuilder;
import com.awakenedredstone.subathon.config.cloth.options.ShortFieldBuilder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.Arrays;
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
                .setTooltip(new TranslatableText("option.subathon.effect_amplifier.description")) // Optional: Shown when the user hover over this option
                .setSaveConsumer(newValue -> Subathon.getConfigData().effectAmplifier = newValue) // Recommended: Called when user save the config
                .build()); // Builds the option entry for cloth config

        general.addEntry(entryBuilder.startStrField(new TranslatableText("option.subathon.channel_name"), Subathon.getConfigData().channelName)
                .setDefaultValue("")
                .setTooltip(new TranslatableText("option.subathon.channel_name.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().channelName = newValue)
                .build());

        general.addEntry(new ShortFieldBuilder(new TranslatableText("option.subathon.sub_amplifier"), Subathon.getConfigData().subModifier)
                .setDefaultValue((short) 1)
                .setTooltip(new TranslatableText("option.subathon.sub_amplifier.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().subModifier = newValue)
                .build());

        general.addEntry(new PasteFieldBuilder(new TranslatableText("option.subathon.client_id"), Subathon.getConfigData().clientId)
                .setTooltip(new TranslatableText("option.subathon.client_id.description"))
                .setSaveConsumer(v -> Subathon.getConfigData().clientId = v)
                .build());

        general.addEntry(new PasteFieldBuilder(new TranslatableText("option.subathon.client_secret"), Subathon.getConfigData().clientSecret)
                .setTooltip(new TranslatableText("option.subathon.client_secret.description"))
                .setSaveConsumer(v -> Subathon.getConfigData().clientSecret = v)
                .build());

        builder.setSavingRunnable(Subathon.config::save);
        return builder.build();
    }

    public static final Function<String, Effect> FUNCTION = (str) -> {
        try {
            return Effect.valueOf(str);
        } catch (Exception var2) {
            return Effect.NONE;
        }
    };
}
