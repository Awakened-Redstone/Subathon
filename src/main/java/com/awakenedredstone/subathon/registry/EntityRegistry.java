package com.awakenedredstone.subathon.registry;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.entity.BigBombEntity;
import com.awakenedredstone.subathon.entity.FireballEntity;
import io.wispforest.owo.registration.reflect.EntityRegistryContainer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.projectile.AbstractFireballEntity;

public class EntityRegistry implements EntityRegistryContainer {

    public static final EntityType<AbstractFireballEntity> FIREBALL = Subathon.createFireballEntity(SpawnGroup.MISC, FireballEntity::new).dimensions(EntityDimensions.fixed(1.0f, 1.0f)).build();
    public static final EntityType<BigBombEntity> BIG_BOMB = Subathon.createProjectileEntity(SpawnGroup.MISC, BigBombEntity::new).dimensions(EntityDimensions.fixed(5.0f, 5.0f)).build();

}
