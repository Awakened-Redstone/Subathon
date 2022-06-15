package com.awakenedredstone.subathon.util;

import com.github.twitch4j.TwitchClient;
import net.minecraft.text.Text;

import static com.awakenedredstone.subathon.Subathon.getConfigData;
import static com.awakenedredstone.subathon.Subathon.usersProgressBar;

public class TwitchUtils {
    public void joinChannels(TwitchClient client) {
        short joinedChannels = 0;
        int totalChannels = getConfigData().channels.size();

        usersProgressBar.setMaxValue(getConfigData().channels.size());
        usersProgressBar.setValue(0);
        usersProgressBar.setName(Text.translatable("text.subathon.load.users", 0, totalChannels));
        usersProgressBar.setVisible(true);

        for (String channel : getConfigData().channels) {
            client.getChat().joinChannel(channel);
            usersProgressBar.setName(Text.translatable("text.subathon.load.users", ++joinedChannels, totalChannels));
            usersProgressBar.setValue(joinedChannels);
        }

        usersProgressBar.setVisible(false);
    }
}
