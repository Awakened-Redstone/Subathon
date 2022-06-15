package com.awakenedredstone.subathon;

import com.awakenedredstone.cubecontroller.util.NbtBuilder;
import com.awakenedredstone.subathon.twitch.InternalData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.io.File;
import java.util.TreeMap;

public class SubathonData extends PersistentState {

    public static SubathonData fromNbt(NbtCompound nbt) {
        int bits = nbt.getInt("bits");
        int subs = nbt.getInt("subs");
        NbtCompound values = nbt.getCompound("nextValues");
        TreeMap<Identifier, Double> nextValues = new TreeMap<>();
        values.getKeys().forEach(key -> nextValues.put(new Identifier(key), values.getDouble(key)));

        Subathon.integration.data = new InternalData(bits, subs, nextValues);
        return new SubathonData();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (Subathon.integration.data != null) {
            nbt.putInt("bits", Subathon.integration.data.bits);
            nbt.putInt("subs", Subathon.integration.data.subs);
            NbtBuilder builder = NbtBuilder.create();
            Subathon.integration.data.nextValues.forEach((identifier, value) -> builder.addDouble(identifier.toString(), value));
            nbt.put("nextValues", builder.build());
        }
        return nbt;
    }

    @Override
    public void save(File file) {
        this.markDirty();
        super.save(file);
    }
}
