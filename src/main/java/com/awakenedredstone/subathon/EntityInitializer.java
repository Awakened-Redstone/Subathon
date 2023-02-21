package com.awakenedredstone.subathon;

import com.awakenedredstone.subathon.entity.FireballEntity;
import io.wispforest.owo.registration.reflect.EntityRegistryContainer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public class EntityInitializer implements EntityRegistryContainer {

    public static final EntityType<FireballEntity> FIREBALL = Subathon.createEntity(SpawnGroup.MISC, FireballEntity::new).dimensions(EntityDimensions.fixed(1.0f, 1.0f)).build();

}
