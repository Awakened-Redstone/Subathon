package com.awakenedredstone.subathon.client.screen;

import com.awakenedredstone.subathon.ChaosMode;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.Utils;
import me.shedaniel.clothconfig2.impl.EasingMethod;
import me.shedaniel.math.Rectangle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Environment(value= EnvType.CLIENT)
public class PercentageScreen extends Screen {
    private PercentageEntryListWidget entryListWidget;
    private final Screen parent;

    public PercentageScreen(Screen parent) {
        super(Text.translatable("gui.subathon.percentage"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.entryListWidget = new PercentageEntryListWidget(this.client);
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
    class PercentageEntryListWidget extends AlwaysSelectedEntryListWidget<Entry> {
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

        public PercentageEntryListWidget(MinecraftClient minecraftClient) {
            super(minecraftClient, PercentageScreen.this.width, PercentageScreen.this.height, 32, PercentageScreen.this.height - 36, 20);
            this.setRenderSelection(false);
            List<Map.Entry<ChaosMode, Double>> chaosCache = Utils.CHAOS_WEIGHTS_CACHE.percentages().entrySet().stream()
                    .sorted(Comparator.comparing(o -> o.getKey().name()))
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();
            for (Map.Entry<ChaosMode, Double> entry : chaosCache) {
                this.addEntry(new PercentageScreen.Entry(entry, Subathon.getEnumDisplayName(Subathon.MOD_ID, ChaosMode.class, entry.getKey()), this, false));
            }

            this.addEntry(new PercentageScreen.Entry(null, Text.empty(), this, true));

            List<Map.Entry<EntityType<?>, Double>> mobsCache = Utils.MOB_WEIGHTS_CACHE.percentages().entrySet().stream()
                    .sorted(Comparator.comparing(o -> o.getKey().getName().getString()))
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();
            for (Map.Entry<EntityType<?>, Double> entry : mobsCache) {
                this.addEntry(new PercentageScreen.Entry(entry, entry.getKey().getName(), this, false));
            }
        }

        @Override
        protected int getScrollbarPositionX() {
            return PercentageScreen.this.width - 6;
        }

        @Override
        public int getRowWidth() {
            return PercentageScreen.this.width - 6;
        }

        @Override
        protected void renderBackground(MatrixStack matrices) {
            PercentageScreen.this.renderBackground(matrices);
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
        private final Map.Entry<?, Double> entry;
        private final Text name;
        private final PercentageEntryListWidget parent;
        private final boolean empty;
        private Rectangle area = new Rectangle(0, 0, 0 , 0);

        Entry(@Nullable Map.Entry<?, Double> entry, Text name, PercentageEntryListWidget parent, boolean empty) {
            this.entry = entry;
            this.name = name;
            this.parent = parent;
            this.empty = empty;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", name);
        }

        public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
            return new Rectangle(x, y, entryWidth, entryHeight - 4);
        }

        public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
            return parent.isMouseOver(mouseX, mouseY) && getEntryArea(x, y, entryWidth, entryHeight).contains(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (empty || entry == null) return;
            int width = parent == null ? PercentageScreen.this.width : parent.getRowWidth();
            MutableText message = Text.translatable("text.subathon.percentage", name, entry.getValue());
            int messageWidth = PercentageScreen.this.textRenderer.getWidth(message);
            int textStart = width < 512 ? (width - messageWidth) / 2 : PercentageScreen.this.width / 32 * 9;
            int rectWidth = Math.max(PercentageScreen.this.width / 8 * 5, messageWidth + 16);
            int start = width < 512 ? (width / 2) - (rectWidth / 2) : textStart - 8;
            if (isMouseInside(mouseX, mouseY, start, y - 1, rectWidth, entryHeight + 7)) {
                Rectangle area = getEntryArea(start, y - 1, rectWidth, entryHeight + 8);
                if (!this.area.equals(area)) this.area = area;
                if (parent != null) parent.thisTimeTarget = area;
            }


            DrawableHelper.drawTextWithShadow(matrices, PercentageScreen.this.textRenderer, message, textStart, y + 5, index % 2 == 0 ? 0xFFFFFF : 0x909090);
        }
    }
}
