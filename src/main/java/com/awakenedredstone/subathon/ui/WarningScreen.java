package com.awakenedredstone.subathon.ui;

import io.wispforest.owo.ui.container.FlowLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class WarningScreen extends BaseScreen<FlowLayout> {

    public WarningScreen() {
        super(FlowLayout.class, "warning_screen");
    }

    @Override
    protected void build(FlowLayout rootComponent) {}
}
