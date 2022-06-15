package com.awakenedredstone.subathon.client.toast;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Environment(value= EnvType.CLIENT)
public class InfoToast implements Toast {
    private static final int MIN_WIDTH = 200;
    private final Type type;
    private Text title;
    private List<OrderedText> lines;
    private long startTime;
    private boolean justUpdated;
    private final int width;

    public InfoToast(Type type, Text title, @Nullable Text description) {
        this(type, title, InfoToast.getTextAsList(description),
                Math.max(160, 30 + Math.max(MinecraftClient.getInstance().textRenderer.getWidth(title),
                        description == null ? 0 : MinecraftClient.getInstance().textRenderer.getWidth(description))));
    }

    public static InfoToast create(MinecraftClient client, Type type, Text title, Text description) {
        TextRenderer textRenderer = client.textRenderer;
        List<OrderedText> list = textRenderer.wrapLines(description, MIN_WIDTH);
        int i = Math.max(MIN_WIDTH, list.stream().mapToInt(textRenderer::getWidth).max().orElse(MIN_WIDTH));
        return new InfoToast(type, title, list, i + 30);
    }

    private InfoToast(Type type, Text title, List<OrderedText> lines, int width) {
        this.type = type;
        this.title = title;
        this.lines = lines;
        this.width = width;
    }

    private static ImmutableList<OrderedText> getTextAsList(@Nullable Text text) {
        return text == null ? ImmutableList.of() : ImmutableList.of(text.asOrderedText());
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public Toast.Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        int k;
        if (this.justUpdated) {
            this.startTime = startTime;
            this.justUpdated = false;
        }
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = this.getWidth();
        if (i == 160 && this.lines.size() <= 1) {
            manager.drawTexture(matrices, 0, 0, 0, 64, i, this.getHeight());
        } else {
            k = this.getHeight() + Math.max(0, this.lines.size() - 1) * 12;
            int m = Math.min(4, k - 28);
            this.drawPart(matrices, manager, i, 0, 0, 28);
            for (int n = 28; n < k - m; n += 10) {
                this.drawPart(matrices, manager, i, 16, n, Math.min(16, k - n - m));
            }
            this.drawPart(matrices, manager, i, 32 - m, k - m, m);
        }
        if (this.lines == null) {
            manager.getClient().textRenderer.draw(matrices, this.title, 18.0f, 12.0f, -256);
        } else {
            manager.getClient().textRenderer.draw(matrices, this.title, 18.0f, 7.0f, -256);
            for (k = 0; k < this.lines.size(); ++k) {
                manager.getClient().textRenderer.draw(matrices, this.lines.get(k), 18.0f, (float)(18 + k * 12), -1);
            }
        }
        return startTime - this.startTime < this.type.displayDuration ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    private void drawPart(MatrixStack matrices, ToastManager manager, int width, int textureV, int y, int height) {
        int i = textureV == 0 ? 20 : 5;
        int j = Math.min(60, width - i);
        manager.drawTexture(matrices, 0, y, 0, 64 + textureV, i, height);
        for (int k = i; k < width - j; k += 64) {
            manager.drawTexture(matrices, k, y, 32, 64 + textureV, Math.min(64, width - k - j), height);
        }
        manager.drawTexture(matrices, width - j, y, 160 - j, 64 + textureV, j, height);
    }

    public void setContent(Text title, @Nullable Text description) {
        this.title = title;
        this.lines = InfoToast.getTextAsList(description);
        this.justUpdated = true;
    }

    public Type getType() {
        return this.type;
    }

    public static void add(ToastManager manager, Type type, Text title, @Nullable Text description) {
        manager.add(new InfoToast(type, title, description));
    }

    public static void show(ToastManager manager, Type type, Text title, @Nullable Text description) {
        InfoToast infoToast = manager.getToast(InfoToast.class, type);
        if (infoToast == null) {
            InfoToast.add(manager, type, title, description);
        } else {
            infoToast.setContent(title, description);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Type(long displayDuration) {}
}
