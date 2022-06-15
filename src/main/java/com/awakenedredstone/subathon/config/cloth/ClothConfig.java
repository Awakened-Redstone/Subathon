package com.awakenedredstone.subathon.config.cloth;

import com.awakenedredstone.cubecontroller.CubeController;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.cloth.options.ByteFieldBuilder;
import com.awakenedredstone.subathon.config.cloth.options.RenderAction;
import com.awakenedredstone.subathon.config.cloth.options.ShortFieldBuilder;
import com.awakenedredstone.subathon.config.cloth.options.ShortListEntry;
import com.awakenedredstone.subathon.twitch.Subscription;
import com.awakenedredstone.subathon.util.SubathonMessageUtils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.awakenedredstone.cubecontroller.util.ConversionUtils.toInt;

public class ClothConfig {
    private final Pattern TWITCH_USERNAME = Pattern.compile("^[a-zA-Z\\d]\\w{0,24}$");
    private final Pattern TIME_PATTERN = Pattern.compile("^(?:(?:(\\d{1,4}):)?([0-5]?\\d):)?([0-5]?\\d)(?:\\.([0-1]?\\d))?$|^\\d{1,10}t$|^\\d{1,10}$");
    private final Pattern TIME_PATTERN_HMST = Pattern.compile("^(?:(?:(\\d{1,4}):)?([0-5]?\\d):)?([0-5]?\\d)(?:\\.([0-1]?\\d))?$");
    private final Pattern TIME_PATTERN_TICKS = Pattern.compile("^\\d{1,10}t$");
    private final Pattern TIME_PATTERN_SECONDS = Pattern.compile("^\\d{1,10}$");

    public Screen build(Screen parent) {
        Subathon.config.loadOrCreateConfig();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        MutableText bruh = Text.literal("§8Bruh");
        int resetButtonWidth = MinecraftClient.getInstance().textRenderer.getWidth(Text.translatable("text.cloth-config.reset_value")) + 6;
        int resetButtonSpace = resetButtonWidth + 4;
        int bruhWidth = MinecraftClient.getInstance().textRenderer.getWidth(bruh) + 2;
        RenderAction<ShortListEntry> renderAction = (matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta, entry) -> {
            if (entry.getValue() == 0 && !entry.isEmpty() && entry.getError().isEmpty()) {
                if (MinecraftClient.getInstance().textRenderer.isRightToLeft()) {
                    textRenderer.drawWithShadow(matrices, bruh, x + resetButtonWidth + (148 - resetButtonSpace) - bruhWidth, y + 6, 0xFFFFFF);
                } else {
                    textRenderer.drawWithShadow(matrices, bruh, x + entryWidth - resetButtonSpace - bruhWidth, y + 6, 0xFFFFFF);
                }
            }
        };

        ConfigBuilder builder = ConfigBuilder.create().setTitle(Text.translatable("title.subathon.config"));
        if (parent != null) {
            builder.setParentScreen(parent);
        }
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.subathon.general"));
        ConfigCategory modifiers = builder.getOrCreateCategory(Text.translatable("category.subathon.modifiers"));
        ConfigCategory client = builder.getOrCreateCategory(Text.translatable("category.subathon.client"));
        ConfigCategory cubeController = builder.getOrCreateCategory(Text.translatable("category.subathon.cube_controller"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        //CubeController sub categories
        SubCategoryBuilder scale = entryBuilder.startSubCategory(Text.translatable("category.subathon.cube_controller.scale"));
        SubCategoryBuilder invoke = entryBuilder.startSubCategory(Text.translatable("category.subathon.cube_controller.invoke"));

        //Modifiers sub categories
        SubCategoryBuilder subs = entryBuilder.startSubCategory(Text.translatable("category.subathon.modifiers.subscribers"));
        SubCategoryBuilder subModifier = entryBuilder.startSubCategory(Text.translatable("category.subathon.modifiers.subscribers.modifiers"));
        SubCategoryBuilder bits = entryBuilder.startSubCategory(Text.translatable("category.subathon.modifiers.bits"));

        //Client sub categories
        SubCategoryBuilder toasts = entryBuilder.startSubCategory(Text.translatable("category.subathon.client.toasts"));

        //Subs sub categories
        subs.add(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.modifiers.sub.enable"), Subathon.getConfigData().enableSubs)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.subathon.modifiers.sub.enable.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().enableSubs = newValue)
                .build());

        subs.add(new ShortFieldBuilder(Text.translatable("option.subathon.modifiers.sub.amplifier"), Subathon.getConfigData().subsPerIncrement)
                .setDefaultValue((short) 1)
                .setTooltip(Text.translatable("option.subathon.modifiers.sub_amplifier.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().subsPerIncrement = newValue)
                .build());

        subs.add(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.modifiers.sub.one_per_gift"), Subathon.getConfigData().onePerGift)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("option.subathon.modifiers.sub.one_per_gift.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().onePerGift = newValue)
                .build());

        Subathon.getConfigData().subModifiers.forEach((k, v) -> {
            subModifier.add(new ShortFieldBuilder(Text.translatable("option.subathon.modifiers.sub.tier", k.getName()), v)
                    .setRender(renderAction)
                    .setMin((short) 0)
                    .setDefaultValue((short) 1)
                    .setTooltip(Text.translatable("option.subathon.modifiers.sub.description"))
                    .setSaveConsumer(newValue -> Subathon.getConfigData().subModifiers.put(k, newValue))
                    .build());
        });

        subs.add(subModifier.build());

        //Bits sub category options

        bits.add(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.modifiers.bits.enable"), Subathon.getConfigData().enableBits)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("option.subathon.modifiers.bits.enable.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().enableBits = newValue)
                .build());

        bits.add(new ShortFieldBuilder(Text.translatable("option.subathon.modifiers.bits.min"), Subathon.getConfigData().bitMin)
                .setMin((short) 1)
                .setDefaultValue((short) 500)
                .setTooltip(Text.translatable("option.subathon.modifiers.bits.min.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().bitMin = newValue)
                .build());

        bits.add(new ShortFieldBuilder(Text.translatable("option.subathon.modifiers.bits.modifier"), Subathon.getConfigData().bitModifier)
                .setDefaultValue((short) 1)
                .setTooltip(Text.translatable("option.subathon.modifiers.bits.min.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().bitModifier = newValue)
                .build());

        bits.add(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.modifiers.bits.cumulative"), Subathon.getConfigData().cumulativeBits)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("option.subathon.modifiers.bits.cumulative.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().cumulativeBits = newValue)
                .build());

        bits.add(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.modifiers.bits.once"), Subathon.getConfigData().onePerCheer)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("option.subathon.modifiers.bits.once.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().onePerCheer = newValue)
                .build());

        bits.add(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.modifiers.bits.cumulative_ignore_min"), Subathon.getConfigData().cumulativeIgnoreMin)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("option.subathon.modifiers.bits.cumulative_ignore_min.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().cumulativeIgnoreMin = newValue)
                .build());

        //General category options
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.general.run_at_server_start"), Subathon.getConfigData().runAtServerStart)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.subathon.general.run_at_server_start.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().runAtServerStart = newValue)
                .build());

        general.addEntry(entryBuilder.startTextField(Text.translatable("option.subathon.general.reset_timer"), SubathonMessageUtils.ticksToTime(Subathon.getConfigData().resetTimer))
                .setDefaultValue("00:00:00.00")
                .setTooltip(Text.translatable("option.subathon.general.reset_timer.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().resetTimer = timeStringToTicks(newValue))
                .setErrorSupplier(v -> {
                    Optional<Text> badFormat = Optional.of(Text.translatable("text.subathon.config.error.not_valid_time"));
                    if (TIME_PATTERN.matcher(v).matches()) {
                        if (TIME_PATTERN_TICKS.matcher(v).matches() && Long.parseLong(v.replace("t", "")) > 719999999) return badFormat;
                        if (TIME_PATTERN_SECONDS.matcher(v).matches() && Long.parseLong(v) > 35999999) return badFormat;
                        return Optional.empty();
                    } else return badFormat;
                })
                .build());

        general.addEntry(entryBuilder.startTextField(Text.translatable("option.subathon.general.update_timer"), SubathonMessageUtils.ticksToTime(Subathon.getConfigData().updateTimer))
                .setDefaultValue("00:00:00.00")
                .setTooltip(Text.translatable("option.subathon.general.update_timer.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().updateTimer = timeStringToTicks(newValue))
                .setErrorSupplier(v -> {
                    Optional<Text> badFormat = Optional.of(Text.translatable("text.subathon.config.error.not_valid_time"));
                    if (TIME_PATTERN.matcher(v).matches()) {
                        if (TIME_PATTERN_TICKS.matcher(v).matches() && Long.parseLong(v.replace("t", "")) > 719999999) return badFormat;
                        if (TIME_PATTERN_SECONDS.matcher(v).matches() && Long.parseLong(v) > 35999999) return badFormat;
                        return Optional.empty();
                    } else return badFormat;
                })
                .build());

        general.addEntry(entryBuilder.startStrList(Text.translatable("option.subathon.general.channels"), Subathon.getConfigData().channels)
                .setDefaultValue(new ArrayList<>())
                .setTooltip(Text.translatable("option.subathon.general.channels.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().channels = newValue)
                .setCellErrorSupplier(value -> {
                    if (TWITCH_USERNAME.matcher(value).matches()) return Optional.empty();
                    else return Optional.of(Text.translatable("text.subathon.config.error.not_valid_twitch_username"));
                })
                .build());

        //CubeController category options
        //TODO: Sync with server
        CubeController.GAME_CONTROL.forEach(control -> {
            String id = control.identifier().toString();
            if (control.valueBased()) {
                scale.add(entryBuilder.startDoubleField(CubeController.getIdentifierTranslation(control.identifier()), Subathon.getConfigData().scales.getOrDefault(id, 1d))
                        .setDefaultValue(1)
                        .setTooltip(Text.translatable("option.subathon.cube_controller.scale.description"))
                        .setSaveConsumer(value -> Subathon.getConfigData().scales.put(id, value))
                        .build());
            }

            if (control.valueBased() && control.hasEvent()) {
                invoke.add(entryBuilder.startBooleanToggle(CubeController.getIdentifierTranslation(control.identifier()), Subathon.getConfigData().invoke.getOrDefault(id, true))
                        .setDefaultValue(true)
                        .setTooltip(Text.translatable("option.subathon.cube_controller.invoke.description"))
                        .setSaveConsumer(value -> Subathon.getConfigData().invoke.put(id, value))
                        .build());
            }
        });

        //Client category options
        client.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.client.broadcast_events"), Subathon.getConfigData().showEventsInChat)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.subathon.client.broadcast_events.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().showEventsInChat = newValue)
                .build());

        client.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.client.show_update_timer"), Subathon.getConfigData().showUpdateTimer)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("option.subathon.client.show_update_timer.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().showUpdateTimer = newValue)
                .build());

        toasts.add(entryBuilder.startBooleanToggle(Text.translatable("option.subathon.client.toasts.enable"), Subathon.getConfigData().showToasts)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.subathon.client.toasts.enable.description"))
                .setSaveConsumer(newValue -> Subathon.getConfigData().showToasts = newValue)
                .build());

        toasts.add(entryBuilder.startIntField(Text.translatable("option.subathon.client.toasts.bits"), Subathon.getConfigData().minBitsForToast)
                .setDefaultValue(1000)
                .setTooltip(Text.translatable("option.subathon.client.toasts.bits.description"))
                .setSaveConsumer(value -> Subathon.getConfigData().minBitsForToast = value)
                .setMin(0)
                .build());

        toasts.add(entryBuilder.startDropdownMenu(Text.translatable("option.subathon.client.toasts.tier"), Subathon.getConfigData().minSubTierForToast,
                        SUB_TIER_FUNCTION, (v) -> Text.literal(v.name()))
                .setSelections(Arrays.stream(Subscription.values()).skip(1).toList())
                .setDefaultValue(Subscription.TIER2)
                .setTooltip(Text.translatable("option.subathon.client.toasts.tier.description"))
                .setSaveConsumer(value -> Subathon.getConfigData().minSubTierForToast = value)
                .setErrorSupplier(subscription -> {
                    if (subscription == Subscription.NONE) return Optional.of(Text.translatable("text.subathon.config.error.not_valid_subscription_tier"));
                    else return Optional.empty();
                })
                .build());

        Subathon.getConfigData().minSubsGiftedForToast.forEach((k, v) -> {
            toasts.add(new ByteFieldBuilder(Text.translatable("option.subathon.client.toasts.gift.tier", k.getName()), v)
                    .setDefaultValue((byte) (9 / k.getValue()))
                    .setRender(renderAction)
                    .setMin((byte) -1)
                    .setTooltip(Text.translatable("option.subathon.client.toasts.gift.tier.description", k.getName()))
                    .setSaveConsumer(value -> Subathon.getConfigData().minSubsGiftedForToast.put(k, value))
                    .build());
        });



        //Add CubeController sub categories
        cubeController.addEntry(scale.build());
        cubeController.addEntry(invoke.build());

        //Add Modifier sub categories
        modifiers.addEntry(subs.build());
        modifiers.addEntry(bits.build());

        //Add Client sub categories
        client.addEntry(toasts.build());

        builder.setSavingRunnable(this::save);
        return builder.build();
    }

    private void save() {
        Subathon.config.save();
    }

    public final Function<String, Subscription> SUB_TIER_FUNCTION = (str) -> {
        try {
            return Subscription.valueOf(str);
        } catch (Exception exception) {
            return Subscription.NONE;
        }
    };

    private int timeStringToTicks(String value) {
        int time = 0;
        Matcher HMST = TIME_PATTERN_HMST.matcher(value);
        if (HMST.matches()) {
            int h = (StringUtils.isNumeric(HMST.group(1)) && StringUtils.isNotBlank(HMST.group(1)) ? Short.parseShort(HMST.group(1)) : 0);
            int m = (StringUtils.isNumeric(HMST.group(2)) && StringUtils.isNotBlank(HMST.group(2)) ? Byte.parseByte(HMST.group(2)) : 0) + h * 60;
            int s = (StringUtils.isNumeric(HMST.group(3)) && StringUtils.isNotBlank(HMST.group(3)) ? Byte.parseByte(HMST.group(3)) : 0) + m * 60;
            time = (StringUtils.isNumeric(HMST.group(4)) && StringUtils.isNotBlank(HMST.group(4)) ? Byte.parseByte(HMST.group(4)) : 0) + s * 20;
        } else if (TIME_PATTERN_TICKS.matcher(value).matches()) {
            time = toInt(Long.parseLong(value.replace("t", "")));
        } else if (TIME_PATTERN_SECONDS.matcher(value).matches()) {
            time = toInt(Long.parseLong(value) * 20);
        }
        return time;
    }
}
