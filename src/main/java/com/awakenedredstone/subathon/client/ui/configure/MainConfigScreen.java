package com.awakenedredstone.subathon.client.ui.configure;

import com.awakenedredstone.subathon.client.ui.BaseScreen;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;
import org.apache.http.util.Asserts;

import java.util.List;

import static com.awakenedredstone.subathon.Subathon.COMMON_CONFIGS;
import static com.awakenedredstone.subathon.client.SubathonClient.CLIENT_CONFIGS;

@Environment(EnvType.CLIENT)
public class MainConfigScreen extends BaseScreen<FlowLayout> {

    public MainConfigScreen() {
        super(FlowLayout.class, "config_screen");
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        Asserts.notNull(client, "client");

        ButtonComponent doneButton = rootComponent.childById(ButtonComponent.class, "done");
        Asserts.notNull(doneButton, "doneButton");
        doneButton.onPress(button -> this.close());

        ButtonComponent commonButton = rootComponent.childById(ButtonComponent.class, "common");
        Asserts.notNull(commonButton, "commonButton");
        if (!client.isIntegratedServerRunning() && !client.isConnectedToRealms() && client.player != null) {
            commonButton.active = false;
            List<TooltipComponent> tooltip = client.textRenderer.wrapLines(Text.translatable("text.subathon.configs.soon.edit"), 200)
                    .stream().map(TooltipComponent::of).toList();
            commonButton.tooltip(tooltip);
        }
        commonButton.onPress(button -> client.setScreen(ConfigScreen.create(COMMON_CONFIGS, this)));

        ButtonComponent serverButton = rootComponent.childById(ButtonComponent.class, "server");
        Asserts.notNull(serverButton, "serverButton");
        {
            serverButton.active = false;
            List<TooltipComponent> tooltip = client.textRenderer.wrapLines(Text.translatable("text.subathon.configs.soon.server"), 200)
                    .stream().map(TooltipComponent::of).toList();
            serverButton.tooltip(tooltip);
        }
        /*if (!client.isIntegratedServerRunning()) {
            serverButton.active = false;
            List<TooltipComponent> tooltip = client.textRenderer.wrapLines(Text.translatable("text.subathon.configs.soon"), 200)
                    .stream().map(TooltipComponent::of).toList();
            serverButton.tooltip(tooltip);
        }
        serverButton.onPress(button -> client.setScreen(ConfigScreen.create(SERVER_CONFIGS, this)));*/

        ButtonComponent clientButton = rootComponent.childById(ButtonComponent.class, "client");
        Asserts.notNull(clientButton, "clientButton");
        clientButton.onPress(button -> client.setScreen(ConfigScreen.create(CLIENT_CONFIGS, this)));

        ButtonComponent rewardsButton = rootComponent.childById(ButtonComponent.class, "rewards");
        Asserts.notNull(rewardsButton, "rewardsButton");
        rewardsButton.onPress(button -> client.setScreen(new RewardScreen()));

        ButtonComponent effectsButton = rootComponent.childById(ButtonComponent.class, "effects");
        Asserts.notNull(effectsButton, "effectsButton");
        if (!client.isIntegratedServerRunning() && client.player != null) {
            commonButton.active = false;
            List<TooltipComponent> tooltip = client.textRenderer.wrapLines(Text.translatable("text.subathon.configs.soon.edit"), 200)
                    .stream().map(TooltipComponent::of).toList();
            commonButton.tooltip(tooltip);
        }
        effectsButton.onPress(button -> client.setScreen(new EffectsScreen()));

        drawSurface(rootComponent, client);
    }
}
