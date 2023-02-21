package old.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.util.math.MatrixStack;

public interface HudRenderCallback {
    Event<HudTick> TICK = EventFactory.createArrayBacked(HudTick.class, (listeners) -> () -> {
        for (HudTick event : listeners) {
            event.onHudTick();
        }
    });

    Event<HudPreTick> PRE_TICK = EventFactory.createArrayBacked(HudPreTick.class, (listeners) -> (paused) -> {
        for (HudPreTick event : listeners) {
            event.onHudPreTick(paused);
        }
    });

    Event<HudRender> RENDER = EventFactory.createArrayBacked(HudRender.class, (listeners) -> (matrixStack, delta) -> {
        for (HudRender event : listeners) {
            event.onHudRender(matrixStack, delta);
        }
    });

    @FunctionalInterface
    public interface HudRender {
        /**
         * Called after rendering the whole hud, which is displayed in game, in a world.
         *
         * @param matrixStack the matrixStack
         * @param tickDelta Progress for linearly interpolating between the previous and current game state
         */
        void onHudRender(MatrixStack matrixStack, float tickDelta);
    }

    @FunctionalInterface
    public interface HudTick {
        /**
         * Called before ticking the whole hud (while not paused), which is displayed in game, in a world.
         */
        void onHudTick();
    }

    @FunctionalInterface
    public interface HudPreTick {
        /**
         * Called before ticking the whole hud, which is displayed in game, in a world.
         */
        void onHudPreTick(boolean paused);
    }
}
