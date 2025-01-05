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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ScrollItem extends Item {
    public static final Identifier ID = Identifier.of("spell_engine", "scroll");
    public static final ScrollItem ITEM = new ScrollItem(new Item.Settings().maxCount(1));

    public ScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Nullable public static boolean applySpell(ItemStack itemStack, Identifier id, Spell spell) {
        var scroll = spell.scroll;
        if (scroll != null && scroll.generate) {
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

    @Nullable public static ItemStack forSpell(Identifier id, Spell spell) {
        var stack = new ItemStack(ITEM);
        var success = applySpell(stack, id, spell);
        return success ? stack : null;
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
