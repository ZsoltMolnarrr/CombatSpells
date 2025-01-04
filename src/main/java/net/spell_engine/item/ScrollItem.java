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
import net.spell_engine.client.gui.SpellTooltip;
import net.spell_power.api.SpellSchool;
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

    @Nullable public static ItemStack forSpell(Identifier id, Spell spell) {
        var scroll = spell.scroll;
        if (scroll != null && scroll.generate) {
            var stack = new ItemStack(ITEM);

            var contentType = spell.school.archetype == SpellSchool.Archetype.ARCHERY
                    ? SpellContainer.ContentType.ARCHERY : SpellContainer.ContentType.MAGIC;
            var container = new SpellContainer(contentType, false, "", 1, List.of(id.toString()));
            stack.set(SpellDataComponents.SPELL_CONTAINER, container);

            var rarity = scroll.custom_rarity;
            if (rarity == null) {
                var ordinal = Math.max(spell.learn.tier - 1, 0); // minimum 0
                rarity = Rarity.values().length > ordinal ? Rarity.values()[ordinal] : Rarity.EPIC;
            }
            stack.set(DataComponentTypes.RARITY, rarity);
            return stack;
        } else {
            return null;
        }
    }

    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        var spell = stack.get(SpellDataComponents.SPELL_SLOT);
        if (spell != null && spell.id() != null && !spell.id().isEmpty()) {
            var id = Identifier.of(spell.id());
            tooltip.add(Text
                    .translatable(SpellTooltip.spellTranslationKey(id))
                    .formatted(Formatting.BOLD)
            );
        }
        if (SpellEngineClient.config.showSpellBindingTooltip) {
            tooltip.add(Text
                    .translatable("item.spell_engine.scroll.table_hint")
                    .formatted(Formatting.GRAY)
            );
        }
    }
}
