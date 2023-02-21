package old.client;

import old.commands.SubathonCommand;
import old.twitch.Subscription;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Environment(EnvType.CLIENT)
public record TwitchEvent(String name, int amount, Subscription tier, SubathonCommand.Events event, String target) {

    public Text getMessage() {
        switch (event) {
            case SUBSCRIPTION -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.sub", name, tier.getName()));
            }
            case RESUBSCRIPTION -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.resub", name, amount, tier.getName()));
            }
            case SUB_GIFT -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.gift", name, amount, tier.getName(), amount != 1 ? "s" : ""));
            }
            case GIFT_USER -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.gift_user", name, amount, tier.getName(), target));
            }
            case CHEER -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.cheer", name, amount));
            }
        }
        return new TranslatableText("gui.subathon.event_logs.error");
    }
}
