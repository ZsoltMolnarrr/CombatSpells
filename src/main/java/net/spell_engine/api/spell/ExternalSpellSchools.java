package net.spell_engine.api.spell;

import net.fabric_extras.ranged_weapon.api.EntityAttributes_RangedWeapon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.spell_power.SpellPowerMod;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

public class ExternalSpellSchools {
    private static RegistryEntry<EntityAttribute> rangedDamageAttribute() {
        if (FabricLoader.getInstance().isModLoaded("ranged_weapon_api")) {
            return EntityAttributes_RangedWeapon.DAMAGE.entry;
        } else {
            return EntityAttributes.GENERIC_ATTACK_DAMAGE; // Vanilla attack damage used as fallback
        }
    }

    public static final SpellSchool PHYSICAL_MELEE = new SpellSchool(SpellSchool.Archetype.MELEE,
            Identifier.of(SpellPowerMod.ID, "physical_melee"),
            0xb3b3b3,
            DamageTypes.PLAYER_ATTACK,
            EntityAttributes.GENERIC_ATTACK_DAMAGE);
    public static final SpellSchool PHYSICAL_RANGED = new SpellSchool(SpellSchool.Archetype.ARCHERY,
            Identifier.of(SpellPowerMod.ID, "physical_ranged"),
            0x805e4d,
            DamageTypes.ARROW,
            rangedDamageAttribute() // Extra compatibility for the absence of `ranged_weapon_api`
    );

    private static boolean initialized = false;
    public static void init() {
        if (initialized) { return; }

        // Sync attack power to client so physical attack damage spells can be estimated.
        // Probably several other mods perform this operation, but its no problem.
        EntityAttributes.GENERIC_ATTACK_DAMAGE.value().setTracked(true);
        PHYSICAL_MELEE.addSource(SpellSchool.Trait.POWER, SpellSchool.Apply.ADD, query -> {
            var power = query.entity().getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);

            var world = query.entity().getWorld();
            var sharpness = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.SHARPNESS);
            if (sharpness.isPresent()) {
                var level = EnchantmentHelper.getLevel(sharpness.get(), query.entity().getMainHandStack());
                power *= 1 + (0.05 * level);
            }
            return power;
        });
        SpellSchools.configureSpellHaste(PHYSICAL_MELEE);
        SpellSchools.register(PHYSICAL_MELEE);

        if (FabricLoader.getInstance().isModLoaded("ranged_weapon_api")) {
            PHYSICAL_RANGED.addSource(SpellSchool.Trait.POWER, SpellSchool.Apply.ADD, query -> {
                var power = query.entity().getAttributeValue(EntityAttributes_RangedWeapon.DAMAGE.entry);

                var world = query.entity().getWorld();
                var powerEnch = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.POWER);
                if (powerEnch.isPresent()) {
                    var level = EnchantmentHelper.getLevel(powerEnch.get(), query.entity().getMainHandStack());
                    power *= 1 + (0.05 * level);
                }
                return power;
            });
            PHYSICAL_RANGED.addSource(SpellSchool.Trait.HASTE, SpellSchool.Apply.ADD, query -> {
                var haste = query.entity().getAttributeValue(EntityAttributes_RangedWeapon.HASTE.entry); // 110
                var rate = EntityAttributes_RangedWeapon.HASTE.asMultiplier(haste);    // For example: 110/100 = 1.1
                return rate - 1;  // 0.1
            });
        }
        SpellSchools.register(PHYSICAL_RANGED);

        initialized = true;
    }
}
