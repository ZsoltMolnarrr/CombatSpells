package net.spell_engine.internals;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.MendingEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.tinyconfig.models.EnchantmentConfig;

import java.util.HashSet;

public class SpellInfinityEnchantment extends Enchantment {
    public EnchantmentConfig config;

    public SpellInfinityEnchantment(Enchantment.Rarity weight, EnchantmentConfig config, EquipmentSlot... slotTypes) {
        super(weight, EnchantmentTarget.BREAKABLE, slotTypes);
        this.config = config;
    }

    public static final Identifier tagId = Identifier.of(SpellEngineMod.ID, "enchant_spell_infinity");
    public static final HashSet<Item> ALLOWED_ITEMS = new HashSet<>();

    public static boolean isEligible(ItemStack stack) {
        return ALLOWED_ITEMS.contains(stack.getItem()) || stack.isIn(TagKey.of(RegistryKeys.ITEM, tagId));
    }

    // MARK: Cost

    public int getMaxLevel() {
        if (!SpellEngineMod.config.spell_cost_item_allowed) {
            return 0;
        }
        return config.max_level;
    }

    public int getMinPower(int level) {
        return config.min_cost + (level - 1) * config.step_cost;
    }

    public int getMaxPower(int level) {
        return super.getMinPower(level) + 50;
    }

    // MARK: Accepting others

    public boolean canAccept(Enchantment other) {
        return (other instanceof MendingEnchantment) ? false : super.canAccept(other);
    }
}
