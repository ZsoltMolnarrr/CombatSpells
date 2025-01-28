package net.spell_engine.compat;

import net.spell_engine.compat.trinkets.TrinketsCompat;

public class CompatFeatures {
    public static void initialize() {
        TrinketsCompat.init();
        CombatRollCompat.init();
    }
}
