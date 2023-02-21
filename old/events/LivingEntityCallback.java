package old.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;

public interface LivingEntityCallback {
    Event<EntityTick> TICK = EventFactory.createArrayBacked(EntityTick.class, (listeners) -> (entity) -> {
        for (EntityTick event : listeners) {
            event.onTick(entity);
        }
    });

    Event<EntityJump> JUMP = EventFactory.createArrayBacked(EntityJump.class, (listeners) -> (entity) -> {
        for (EntityJump event : listeners) {
            event.onJump(entity);
        }
    });

    @FunctionalInterface
    public interface EntityJump {
        /**
         * Called when an entity jumps.
         *
         * @param entity the entity
         */
        void onJump(LivingEntity entity);
    }

    @FunctionalInterface
    public interface EntityTick {
        /**
         * Called when an entity ticks.
         *
         * @param entity the entity
         */
        void onTick(LivingEntity entity);
    }
}
