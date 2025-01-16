package net.spell_engine.api.spell.event;

import it.unimi.dsi.fastutil.Function;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.SpellHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomSpellHandler {
    public static final Map<Identifier, Function<Data, Boolean>> handlers = new HashMap<>();

    public record Data(
            PlayerEntity caster,
            List<Entity> targets,
            ItemStack itemStack,
            SpellCast.Action action,
            float progress,
            SpellHelper.ImpactContext impactContext) { }

    public static void register(Identifier spellId, Function<Data, Boolean> handler) {
        handlers.put(spellId, handler);
    }
}
