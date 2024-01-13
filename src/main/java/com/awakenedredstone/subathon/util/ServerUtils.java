package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import net.minecraft.text.Text;

public class ServerUtils {
    public static void broadcast(Text message) {
        Subathon.getServer().getPlayerManager().broadcast(message, false);
    }

    public static void broadcast(Text message, boolean overlay) {
        Subathon.getServer().getPlayerManager().broadcast(message, overlay);
    }
}
