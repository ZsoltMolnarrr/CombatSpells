package net.spell_engine.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.spell_engine.api.spell.*;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.internals.SpellContainerHelper;
import net.spell_engine.internals.SpellRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ScrollItem extends Item {
    public static final Identifier ID = Identifier.of("spell_engine", "scroll");

    public ScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Nullable public static boolean applySpell(ItemStack itemStack, Identifier id, Spell spell, boolean requirePool) {
        var scroll = spell.scroll;
        if (scroll != null && scroll.generate) {
//            var poolId = SpellRegistry.pools.entrySet().stream() // TODO: Disable generate for low tier spells
//                    .sorted(Comparator.comparing(a -> a.getKey().toString()))
//                    .filter(e -> e.getValue().spellIds().contains(id))
//                    .map(Map.Entry::getKey)
//                    .findFirst()
//                    .orElse(null);
//            // At least one compatible pool is required when generating for loot
//            if (requirePool && poolId == null)  {
//                // There is no spell pool to use this spell
//                // No scroll should be generated
//                return false;
//            }

//            if (poolId != null) {
//                var translationKey = "item." + poolId.getNamespace() + "." + poolId.getPath() + ".spell_scroll";
//                if (I18n.hasTranslation(translationKey)) {
//                    itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable(translationKey));
//                }
//            }
            itemStack.set(SpellDataComponents.SPELL_CONTAINER, SpellContainerHelper.create(id, spell, itemStack.getItem()));

            var rarity = scroll.custom_rarity;
            if (rarity == null) {
                var ordinal = Math.max(spell.learn.tier - 1, 0); // minimum 0
                rarity = Rarity.values().length > ordinal ? Rarity.values()[ordinal] : Rarity.EPIC;
            }
            itemStack.set(DataComponentTypes.RARITY, rarity);

            return true;
        } else {
            return false;
        }
    }

    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (SpellEngineClient.config.showSpellBindingTooltip) {
            tooltip.add(Text
                    .translatable("item.spell_engine.scroll.table_hint")
                    .formatted(Formatting.GRAY)
            );
        }
    }
}
