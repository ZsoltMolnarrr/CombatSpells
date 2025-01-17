package net.spell_engine.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.util.function.Function;

public class AttackRangeCompat {
    private Function<PlayerEntity, Float> source;
    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("bettercombat")) {

        } else {

        }
    }
}
