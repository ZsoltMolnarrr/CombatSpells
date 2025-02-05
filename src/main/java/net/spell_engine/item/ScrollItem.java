package net.spell_engine.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.spell_engine.api.spell.*;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ScrollItem extends Item {
    public static final Identifier ID = Identifier.of("spell_engine", "scroll");

    public ScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Nullable public static boolean applySpell(ItemStack itemStack, RegistryEntry<Spell> spellEntry, boolean requirePool) {
        var spell = spellEntry.value();
        if (spell.active == null) { return false; }
        var scroll = spell.active.scroll;
        if (scroll != null) {
//            if (poolId != null) {
//                var translationKey = "item." + poolId.getNamespace() + "." + poolId.getPath() + ".spell_scroll";
//                if (I18n.hasTranslation(translationKey)) {
//                    itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable(translationKey));
//                }
//            }
            itemStack.set(SpellDataComponents.SPELL_CONTAINER, SpellContainerHelper.create(spellEntry, itemStack.getItem()));

            var rarity = scroll.custom_rarity;
            if (rarity == null) {
                var ordinal = Math.max(spell.tier - 1, 0); // minimum 0
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
