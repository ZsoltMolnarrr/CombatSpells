package net.spell_engine.compat;

import net.combat_roll.api.event.ServerSideRollEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.spell_engine.internals.SpellTriggers;

public class CombatRollCompat {
    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("combat_roll")) {
            ServerSideRollEvents.PLAYER_START_ROLLING.register((player, roll) -> {
                SpellTriggers.onRoll(player);
            });
        }
    }
}
