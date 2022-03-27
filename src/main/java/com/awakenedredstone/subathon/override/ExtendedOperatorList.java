package com.awakenedredstone.subathon.override;

import com.awakenedredstone.subathon.mixin.ServerConfigEntryMixin;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.OperatorList;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

public class ExtendedOperatorList extends OperatorList {

    public ExtendedOperatorList(File file) {
        super(file);
    }

    public UUID[] getUUIDs() {
        return this.values().stream().map(v -> ((ServerConfigEntryMixin<GameProfile>) v).getKey()).filter(Objects::nonNull).map(GameProfile::getId).toArray(UUID[]::new);
    }
}
