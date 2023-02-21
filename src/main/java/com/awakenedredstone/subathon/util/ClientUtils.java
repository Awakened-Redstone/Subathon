package com.awakenedredstone.subathon.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

@Environment(EnvType.CLIENT)
public class ClientUtils {
    public static KeyBinding addKeybind(String name, InputUtil.Type type, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.subathon." + name, type, code, "category.subathon.keybinds"));
    }

    public static KeyBinding addKeybind(String name, int code) {
        return addKeybind(name, InputUtil.Type.KEYSYM, code);
    }
}
