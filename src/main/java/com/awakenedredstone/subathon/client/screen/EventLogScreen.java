package com.awakenedredstone.subathon.client.screen;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.client.TwitchEvent;
import me.shedaniel.clothconfig2.impl.EasingMethod;
import me.shedaniel.math.Rectangle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import static com.awakenedredstone.subathon.commands.SubathonCommand.Events.GIFT_USER;

@Environment(value= EnvType.CLIENT)
public class EventLogScreen extends Screen {
    private EventEntryListWidget entryListWidget;
    private final Screen parent;

    public EventLogScreen(Screen parent) {
        super(Text.translatable("gui.subathon.event_logs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.entryListWidget = new EventEntryListWidget(this.client);
        this.addSelectableChild(entryListWidget);
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 155, this.height - 28, 150, 20, ScreenTexts.DONE, button -> this.client.setScreen(this.parent)));
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 5, this.height - 28, 150, 20, Text.translatable("gui.subathon.clear_logs"), button ->
                client.setScreen(new ConfirmScreen(bool -> {
                        if (bool) {
                            SubathonClient.events.clear();
                            this.entryListWidget = new EventEntryListWidget(this.client);
                        }
                        this.client.setScreen(this);
                    }, Text.translatable("gui.subathon.clear_logs.question"), Text.translatable("gui.subathon.clear_logs.message"),
                        Text.translatable("gui.subathon.clear_logs"), Text.translatable("gui.cancel")))));
        super.init();
    }

    public void addToList(TwitchEvent event) {
        this.entryListWidget.addToList(event);
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
    class EventEntryListWidget extends AlwaysSelectedEntryListWidget<Entry> {
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

        public EventEntryListWidget(MinecraftClient minecraftClient) {
            super(minecraftClient, EventLogScreen.this.width, EventLogScreen.this.height, 32, EventLogScreen.this.height - 36, 20);
            this.setRenderSelection(false);
            for (TwitchEvent event : SubathonClient.events) {
                this.addEntry(new EventLogScreen.Entry(event, this));
            }
        }

        @Override
        protected int getScrollbarPositionX() {
            return EventLogScreen.this.width - 6;
        }

        @Override
        public int getRowWidth() {
            return EventLogScreen.this.width - 6;
        }

        @Override
        protected void renderBackground(MatrixStack matrices) {
            EventLogScreen.this.renderBackground(matrices);
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

        public void addToList(TwitchEvent event) {
            this.addEntry(new EventLogScreen.Entry(event, this));
        }

        public static double ease(double start, double end, double amount, EasingMethod easingMethod) {
            return start + (end - start) * easingMethod.apply(amount);
        }
    }

    @Environment(value=EnvType.CLIENT)
    class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        private final TwitchEvent event;
        private final EventEntryListWidget parent;
        private Rectangle area = new Rectangle(0, 0, 0 , 0);

        Entry(TwitchEvent event, EventEntryListWidget parent) {
            this.event = event;
            this.parent = parent;
        }

        @Override
        public Text getNarration() {
            return Text.translatable("narrator.select", event.getMessage());
        }

        public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
            return new Rectangle(x, y, entryWidth, entryHeight - 4);
        }

        public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
            return parent.isMouseOver(mouseX, mouseY) && getEntryArea(x, y, entryWidth, entryHeight).contains(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if ((button == 1 && area.contains(mouseX, mouseY) && !event.message().isEmpty() && event.event() != GIFT_USER) || button == 0) {
                EventLogScreen.this.client.setScreen(new EventMessageScreen(EventLogScreen.this, event, button == 1));
                EventLogScreen.this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int width = parent == null ? EventLogScreen.this.width : parent.getRowWidth();
            int messageWidth = EventLogScreen.this.textRenderer.getWidth(event.getMessage());
            int textStart = width < 512 ? (width - messageWidth) / 2 : EventLogScreen.this.width / 32 * 9;
            int rectWidth = Math.max(EventLogScreen.this.width / 8 * 5, messageWidth + 16);
            int start = width < 512 ? (width / 2) - (rectWidth / 2) : textStart - 8;
            if (isMouseInside(mouseX, mouseY, start, y - 1, rectWidth, entryHeight + 7)) {
                Rectangle area = getEntryArea(start, y - 1, rectWidth, entryHeight + 8);
                if (!this.area.equals(area)) this.area = area;
                if (parent != null) parent.thisTimeTarget = area;
                Style style = EventLogScreen.this.textRenderer.getTextHandler().getStyleAt(event.getMessage(), 0);
                if (style != null && style.getHoverEvent() != null) EventLogScreen.this.renderTextHoverEffect(matrices, style, mouseX, mouseY);
            }


            DrawableHelper.drawTextWithShadow(matrices, EventLogScreen.this.textRenderer, event.getMessage(), textStart, y + 5, index % 2 == 0 ? 0xFFFFFF : 0x909090);
        }
    }
}
