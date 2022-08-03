package com.awakenedredstone.subathon.client.screen;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.TwitchUtils;
import com.github.twitch4j.graphql.internal.FetchCommunityPointsSettingsQuery;
import com.github.twitch4j.helix.domain.User;
import me.shedaniel.clothconfig2.impl.EasingMethod;
import me.shedaniel.math.Rectangle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Environment(value= EnvType.CLIENT)
public class PickRewardScreen extends Screen {
    private RewardEntryListWidget entryListWidget;
    private final Screen parent;
    private final String channelName;

    public PickRewardScreen(Screen parent, String channelName) {
        super(Text.translatable("gui.subathon.pick_reward"));
        this.parent = parent;
        this.channelName = channelName;
    }

    @Override
    protected void init() {
        this.entryListWidget = new RewardEntryListWidget(this.client);
        this.addSelectableChild(entryListWidget);
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 150, this.height - 28, 300, 20, ScreenTexts.DONE, button -> this.client.setScreen(this.parent)));
        super.init();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        entryListWidget.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Environment(value=EnvType.CLIENT)
    class RewardEntryListWidget extends AlwaysSelectedEntryListWidget<Entry> {
        private boolean hasCurrent;
        private int currentX;
        private int currentY;
        private int currentWidth;
        private int currentHeight;
        public Rectangle target;
        public Rectangle thisTimeTarget;
        public long lastTouch;
        public long start;
        public long duration;

        public RewardEntryListWidget(MinecraftClient minecraftClient) {
            super(minecraftClient, PickRewardScreen.this.width, PickRewardScreen.this.height, 32, PickRewardScreen.this.height - 36, 20);
            this.setRenderSelection(false);
            FetchCommunityPointsSettingsQuery.Channel channel = Subathon.integration.getTwitchClient().getGraphQL().fetchChannelPointRewards(null, channelName).execute().channel();
            if (channel == null || channel.communityPointsSettings() == null) return;
            for (FetchCommunityPointsSettingsQuery.CustomReward customReward : channel.communityPointsSettings().customRewards()) {
                this.addEntry(new PickRewardScreen.Entry(customReward, this));
            }
        }

        @Override
        protected int getScrollbarPositionX() {
            return PickRewardScreen.this.width - 6;
        }

        @Override
        public int getRowWidth() {
            return PickRewardScreen.this.width - 6;
        }

        @Override
        protected void renderBackground(MatrixStack matrices) {
            PickRewardScreen.this.renderBackground(matrices);
        }

        @Override
        protected void renderList(MatrixStack matrices, int x, int y, int mouseX, int mouseY, float delta) {
            thisTimeTarget = null;
            if (hasCurrent) {
                long timePast = System.currentTimeMillis() - lastTouch;
                int alpha = timePast <= 200 ? 255 : (int) Math.ceil(255 - Math.min(timePast - 200, 500f) / 500f * 255.0);
                alpha = (alpha * 36 / 255) << 24;
                fillGradient(matrices, currentX, currentY, currentX + currentWidth, currentY + currentHeight, 0xFFFFFF | alpha, 0xFFFFFF | alpha);
            }
            super.renderList(matrices, x, y, mouseX, mouseY, delta);
            if (thisTimeTarget != null && isMouseOver(mouseX, mouseY)) {
                lastTouch = System.currentTimeMillis();
            }
            if (thisTimeTarget != null && !thisTimeTarget.equals(target)) {
                if (!hasCurrent) {
                    currentX = thisTimeTarget.x;
                    currentY = thisTimeTarget.y;
                    currentWidth = thisTimeTarget.width;
                    currentHeight = thisTimeTarget.height;
                    hasCurrent = true;
                }
                target = thisTimeTarget.clone();
                start = lastTouch;
                this.duration = 40;
            } else if (hasCurrent && target != null) {
                long timePast = System.currentTimeMillis() - start;
                currentX = (int) ease(currentX, target.x, Math.min(timePast / (double) duration * delta * 3, 1), EasingMethod.EasingMethodImpl.LINEAR);
                currentY = (int) ease(currentY, target.y, Math.min(timePast / (double) duration * delta * 3, 1), EasingMethod.EasingMethodImpl.LINEAR);
                currentWidth = (int) ease(currentWidth, target.width, Math.min(timePast / (double) duration * delta * 3, 1), EasingMethod.EasingMethodImpl.LINEAR);
                currentHeight = (int) ease(currentHeight, target.height, Math.min(timePast / (double) duration * delta * 3, 1), EasingMethod.EasingMethodImpl.LINEAR);
            }
        }

        public static double ease(double start, double end, double amount, EasingMethod easingMethod) {
            return start + (end - start) * easingMethod.apply(amount);
        }
    }

    @Environment(value=EnvType.CLIENT)
    class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        private final FetchCommunityPointsSettingsQuery.CustomReward reward;
        private final RewardEntryListWidget parent;
        private Rectangle area = new Rectangle(0, 0, 0 , 0);

        Entry(FetchCommunityPointsSettingsQuery.CustomReward reward, RewardEntryListWidget parent) {
            this.reward = reward;
            this.parent = parent;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", reward.title());
        }

        public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
            return new Rectangle(x, y, entryWidth, entryHeight - 4);
        }

        public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
            return parent.isMouseOver(mouseX, mouseY) && getEntryArea(x, y, entryWidth, entryHeight).contains(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && area.contains(mouseX, mouseY)) {
                List<User> users = new TwitchUtils.Builder().build().getUsersInfo(null, List.of(channelName)).execute().getUsers();
                if (users.size() == 0) {
                    client.getToastManager().add(SystemToast.create(client, SystemToast.Type.PACK_LOAD_FAILURE,
                            Text.translatable("text.subathon.error.invalid_user"), Text.translatable("text.subathon.error.invalid_user.description")));
                } else {
                    Subathon.getConfigData().rewardId.put(users.get(0).getId(), reward.id());
                    Subathon.config.save();
                }
                PickRewardScreen.this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                PickRewardScreen.this.close();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            String formattedCost = format(reward.cost());
            MutableText cost = Text.translatable("text.subathon.reward_cost", formattedCost);
            int costWidth = PickRewardScreen.this.textRenderer.getWidth(cost);

            int width = parent == null ? PickRewardScreen.this.width : parent.getRowWidth();
            MutableText message = Text.literal(reward.isSubOnly() ? "⭐ " : "" + reward.title());
            int messageWidth = PickRewardScreen.this.textRenderer.getWidth(message);
            int textStart = width < 512 ? (width - messageWidth) / 2 : PickRewardScreen.this.width / 32 * 9;
            int rectWidth = Math.max(PickRewardScreen.this.width / 8 * 5 + costWidth, messageWidth + 16 + costWidth);
            int start = width < 512 ? (width / 2) - (rectWidth / 2) : textStart - 8 - costWidth;
            if (isMouseInside(mouseX, mouseY, start, y - 1, rectWidth, entryHeight + 7)) {
                Rectangle area = getEntryArea(start, y - 1, rectWidth, entryHeight + 8);
                if (!this.area.equals(area)) this.area = area;
                if (parent != null) parent.thisTimeTarget = area;
            }

            int color = 0xFFFFFF;
            if (reward.isPaused()) color = 0x909090;
            if (!reward.isEnabled()) color = 0x900000;

            DrawableHelper.drawTextWithShadow(matrices, PickRewardScreen.this.textRenderer, cost, textStart - costWidth, y + 5, color);
            DrawableHelper.drawTextWithShadow(matrices, PickRewardScreen.this.textRenderer, message, textStart, y + 5, color);
        }
    }

    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "B");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "q");
        suffixes.put(1_000_000_000_000_000_000L, "Q");
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    public static String format(long value) {
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value);

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }
}
