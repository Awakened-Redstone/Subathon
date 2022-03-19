package com.awakenedredstone.subathon.config.cloth;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.Effect;
import com.awakenedredstone.subathon.config.MessageMode;
import com.awakenedredstone.subathon.config.cloth.options.ButtonFieldBuilder;
import com.awakenedredstone.subathon.config.cloth.options.PasteFieldBuilder;
import com.awakenedredstone.subathon.config.cloth.options.RenderAction;
import com.awakenedredstone.subathon.config.cloth.options.ShortFieldBuilder;
import com.awakenedredstone.subathon.twitch.Bot;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class ClothConfig {

    public Screen build(Screen parent) {
        Subathon.generateConfig();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        LiteralText bruh = new LiteralText("§8Bruh");
        int resetButtonWidth = MinecraftClient.getInstance().textRenderer.getWidth(new TranslatableText("text.cloth-config.reset_value")) + 6;
        int resetButtonSpace = resetButtonWidth + 4;
        int bruhWidth = MinecraftClient.getInstance().textRenderer.getWidth(bruh) + 2;
        RenderAction renderAction = (matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta, entry) -> {
            if (entry.getValue() == 0 && !entry.isEmpty() && entry.getError().isEmpty()) {
                if (MinecraftClient.getInstance().textRenderer.isRightToLeft()) {
                    textRenderer.drawWithShadow(matrices, bruh, x + resetButtonWidth + (148 - resetButtonSpace) - bruhWidth, y + 6, 0xFFFFFF);
                } else {
                    textRenderer.drawWithShadow(matrices, bruh, x + entryWidth - resetButtonSpace - bruhWidth, y + 6, 0xFFFFFF);
                }
            }
        };

        ConfigBuilder builder = ConfigBuilder.create().setTitle(new TranslatableText("title.subathon.config"));
        if (parent != null) {
            builder.setParentScreen(parent);
        }
        ConfigCategory general = builder.getOrCreateCategory(new TranslatableText("category.subathon.general"));
        ConfigCategory modifiers = builder.getOrCreateCategory(new TranslatableText("category.subathon.modifiers"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        //Subs sub category options
        SubCategoryBuilder subs = entryBuilder.startSubCategory(new TranslatableText("category.subathon.subscribers"));
        SubCategoryBuilder subModifier = entryBuilder.startSubCategory(new TranslatableText("category.subathon.subscribers.modifiers"));

        subs.add(entryBuilder.startBooleanToggle(new TranslatableText("option.subathon.sub.enable"), Subathon.getConfigData().enableSubs)
                .setDefaultValue(true)
                .setTooltip(new TranslatableText("option.subathon.sub.enable.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().enableSubs = newValue)
                /*.setErrorSupplier(value -> {
                    if (!value && !Subathon.getConfigData().enableBits) return Optional.of(new TranslatableText("text.subathon.error.requires_one_enabled"));
                    return Optional.empty();
                })*/
                .build());

        subs.add(new ShortFieldBuilder(new TranslatableText("option.subathon.sub_amplifier"), Subathon.getConfigData().subsPerIncrement)
                .setDefaultValue((short) 1)
                .setTooltip(new TranslatableText("option.subathon.sub_amplifier.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().subsPerIncrement = newValue)
                .build());

        subs.add(entryBuilder.startBooleanToggle(new TranslatableText("option.subathon.sub.one_per_gift"), Subathon.getConfigData().onePerGift)
                .setDefaultValue(false)
                .setTooltip(new TranslatableText("option.subathon.sub.one_per_gift.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().onePerGift = newValue)
                .build());

        Subathon.getConfigData().subModifiers.forEach((k, v) -> {
            subModifier.add(new ShortFieldBuilder(new TranslatableText("option.subathon.sub." + k), v)
                    .setRender(renderAction)
                    .setMin((short) 0)
                    .setDefaultValue((short) 1)
                    .setTooltip(new TranslatableText("option.subathon.sub.description"))
                    .setSaveConsumer(newValue -> Subathon.getConfigData().subModifiers.put(k, newValue))
                    .build());
        });

        subs.add(subModifier.build());

        //Bits sub category options
        SubCategoryBuilder bits = entryBuilder.startSubCategory(new TranslatableText("category.subathon.bits"));

        bits.add(entryBuilder.startBooleanToggle(new TranslatableText("option.subathon.bits.enable"), Subathon.getConfigData().enableBits)
                .setDefaultValue(false)
                .setTooltip(new TranslatableText("option.subathon.bits.enable.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().enableBits = newValue)
                /*.setErrorSupplier(value -> {
                    if (!value && !Subathon.getConfigData().enableSubs) return Optional.of(new TranslatableText("text.subathon.error.requires_one_enabled"));
                    return Optional.empty();
                })*/
                .build());

        bits.add(new ShortFieldBuilder(new TranslatableText("option.subathon.bits.min"), Subathon.getConfigData().bitMin)
                .setMin((short) 1)
                .setDefaultValue((short) 500)
                .setTooltip(new TranslatableText("option.subathon.bits.min.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().bitMin = newValue)
                .build());

        bits.add(new ShortFieldBuilder(new TranslatableText("option.subathon.bits.modifier"), Subathon.getConfigData().bitModifier)
                .setDefaultValue((short) 1)
                .setTooltip(new TranslatableText("option.subathon.bits.min.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().bitModifier = newValue)
                .build());

        bits.add(entryBuilder.startBooleanToggle(new TranslatableText("option.subathon.bits.cumulative"), Subathon.getConfigData().cumulativeBits)
                .setDefaultValue(false)
                .setTooltip(new TranslatableText("option.subathon.bits.cumulative.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().cumulativeBits = newValue)
                .build());

        bits.add(entryBuilder.startBooleanToggle(new TranslatableText("option.subathon.bits.once"), Subathon.getConfigData().onePerCheer)
                .setDefaultValue(false)
                .setTooltip(new TranslatableText("option.subathon.bits.once.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().onePerCheer = newValue)
                .build());

        bits.add(entryBuilder.startBooleanToggle(new TranslatableText("option.subathon.bits.cumulative_ignore_min"), Subathon.getConfigData().cumulativeIgnoreMin)
                .setDefaultValue(false)
                .setTooltip(new TranslatableText("option.subathon.bits.cumulative_ignore_min.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().cumulativeIgnoreMin = newValue)
                .build());

        //General category options
        general.addEntry(entryBuilder.startDropdownMenu(new TranslatableText("option.subathon.mode"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(Effect.valueOf(Subathon.getConfigData().mode), EFFECT_FUNCTION, (effect) -> new LiteralText(effect.toString())),
                        DropdownMenuBuilder.CellCreatorBuilder.of())
                .setDefaultValue(Effect.JUMP)
                .setTooltip(new TranslatableText("option.subathon.mode.description"))
                .setSelections(Arrays.stream(Effect.values()).toList())
                .setSaveConsumer(mode -> Subathon.getConfigData().mode = mode.toString())
                .build());

        general.addEntry(entryBuilder.startDropdownMenu(new TranslatableText("option.subathon.message_mode"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(MessageMode.valueOf(Subathon.getConfigData().messageMode), MESSAGE_MODE_FUNCTION, (effect) -> new LiteralText(effect.toString())),
                        DropdownMenuBuilder.CellCreatorBuilder.of())
                .setDefaultValue(MessageMode.OVERLAY)
                .setTooltip(new TranslatableText("option.subathon.message_mode.description"))
                .setSelections(Arrays.stream(MessageMode.values()).toList())
                .setSaveConsumer(mode -> Subathon.getConfigData().messageMode = mode.toString())
                .build());

        general.addEntry(entryBuilder.startFloatField(new TranslatableText("option.subathon.effect_amplifier"), Subathon.getConfigData().effectAmplifier)
                .setDefaultValue(0.1f)
                .setTooltip(new TranslatableText("option.subathon.effect_amplifier.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().effectAmplifier = newValue)
                .build());

        general.addEntry(new PasteFieldBuilder(new TranslatableText("option.subathon.auth_data"), Subathon.auth.getJsonString())
                .setDefaultValue("{}")
                .setType("JSON")
                .setTooltip(new TranslatableText("option.subathon.auth_data.description"))
                .setSaveConsumer(Subathon.auth::fromString)
                .build());

        general.addEntry(new ButtonFieldBuilder(new TranslatableText("option.subathon.auth"))
                .setTooltip(new TranslatableText("option.subathon.auth.description"))
                .setPressAction((button) -> Util.getOperatingSystem().open(Bot.getAuthenticationUrl(List.of("channel:read:subscriptions", "bits:read"), null)))
                .build());

        //Modifier category options
        modifiers.addEntry(subs.build());
        modifiers.addEntry(bits.build());

        builder.setSavingRunnable(this::save);
        return builder.build();
    }

    private void save() {
        Subathon.config.save();
        Subathon.auth.save();
    }

    public static final Function<String, Effect> EFFECT_FUNCTION = (str) -> {
        try {
            return Effect.valueOf(str);
        } catch (Exception var2) {
            return Effect.NONE;
        }
    };

    public static final Function<String, MessageMode> MESSAGE_MODE_FUNCTION = (str) -> {
        try {
            return MessageMode.valueOf(str);
        } catch (Exception var2) {
            return MessageMode.CHAT;
        }
    };
}
