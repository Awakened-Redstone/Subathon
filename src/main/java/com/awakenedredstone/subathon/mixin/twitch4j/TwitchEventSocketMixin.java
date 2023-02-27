package com.awakenedredstone.subathon.mixin.twitch4j;

import com.awakenedredstone.subathon.Subathon;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.socket.TwitchEventSocket;
import com.github.twitch4j.eventsub.socket.events.EventSocketDeleteSubscriptionSuccessEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TwitchEventSocket.class, remap = false)
public class TwitchEventSocketMixin {
    @Shadow(remap = false) @Final private @NotNull EventManager eventManager;

    @Inject(method = "lambda$close$3", at = @At(value = "INVOKE", target = "Lcom/github/twitch4j/helix/TwitchHelix;deleteEventSubSubscription(Ljava/lang/String;Ljava/lang/String;)Lcom/netflix/hystrix/HystrixCommand;", shift = At.Shift.AFTER), remap = false)
    private void subathon$addDeleteSubSuccessEvent0(@Coerce EventSubSubscription sub, CallbackInfo ci) {
        eventManager.publish(new EventSocketDeleteSubscriptionSuccessEvent(sub, (TwitchEventSocket) (Object) this));
        Subathon.LOGGER.info("Triggering event of {} from close", sub.getRawType());
    }

    @Inject(method = "lambda$unregister$5", at = @At(value = "INVOKE", target = "Lcom/github/twitch4j/helix/TwitchHelix;deleteEventSubSubscription(Ljava/lang/String;Ljava/lang/String;)Lcom/netflix/hystrix/HystrixCommand;", shift = At.Shift.AFTER), remap = false)
    private void subathon$addDeleteSubSuccessEvent1(EventSubSubscription sub, CallbackInfo ci) {
        eventManager.publish(new EventSocketDeleteSubscriptionSuccessEvent(sub, (TwitchEventSocket) (Object) this));
        Subathon.LOGGER.info("Triggering event of {} from unregister", sub.getRawType());
    }

    @Inject(method = "lambda$onInitialConnection$6", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;trace(Ljava/lang/String;Ljava/lang/Object;)V", shift = At.Shift.AFTER), remap = false)
    private void subathon$addDeleteSubSuccessEvent2(EventSubSubscription old, String websocketId, CallbackInfo ci) {
        eventManager.publish(new EventSocketDeleteSubscriptionSuccessEvent(old, (TwitchEventSocket) (Object) this));
        Subathon.LOGGER.info("Triggering event of {} from onInitialConnection", old.getRawType());
    }
}
