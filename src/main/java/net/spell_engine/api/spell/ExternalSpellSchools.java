package net.spell_engine.api.spell;

import net.fabric_extras.ranged_weapon.api.EntityAttributes_RangedWeapon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.util.Identifier;
import net.spell_power.SpellPowerMod;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

public class ExternalSpellSchools {
    private static EntityAttribute rangedDamageAttribute() {
        if (FabricLoader.getInstance().isModLoaded("ranged_weapon_api")) {
            return EntityAttributes_RangedWeapon.DAMAGE.attribute;
        } else {
            return EntityAttributes.GENERIC_ATTACK_DAMAGE; // Vanilla attack damage used as fallback
        }
    }

    public static final SpellSchool PHYSICAL_MELEE = new SpellSchool(SpellSchool.Archetype.MELEE,
            new Identifier(SpellPowerMod.ID, "physical_melee"),
            0xb3b3b3,
            DamageTypes.PLAYER_ATTACK,
            EntityAttributes.GENERIC_ATTACK_DAMAGE);
    public static final SpellSchool PHYSICAL_RANGED = new SpellSchool(SpellSchool.Archetype.ARCHERY,
            new Identifier(SpellPowerMod.ID, "physical_ranged"),
            0x805e4d,
            DamageTypes.ARROW,
            rangedDamageAttribute() // Extra compatibility for the absence of `ranged_weapon_api`
    );

    private static boolean initialized = false;
    public static void initialize() {
        if (initialized) { return; }

        // Sync attack power to client so physical attack damage spells can be estimated.
        // Probably several other mods perform this operation, but its no problem.
        EntityAttributes.GENERIC_ATTACK_DAMAGE.setTracked(true);
        PHYSICAL_MELEE.attributeManagement = SpellSchool.Manage.EXTERNAL;
        PHYSICAL_MELEE.addSource(SpellSchool.Trait.POWER, SpellSchool.Apply.ADD, query -> {
            var power = query.entity().getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            var level = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, query.entity().getMainHandStack());
            power *= 1 + (0.05 * level);
            // TODO: Maybe consider attack speed
            return power;
        });
        SpellSchools.configureSpellHaste(PHYSICAL_MELEE);
        SpellSchools.register(PHYSICAL_MELEE);

        PHYSICAL_RANGED.attributeManagement = SpellSchool.Manage.EXTERNAL;
        if (FabricLoader.getInstance().isModLoaded("ranged_weapon_api")) {
            PHYSICAL_RANGED.addSource(SpellSchool.Trait.POWER, SpellSchool.Apply.ADD, query -> {
                var power = query.entity().getAttributeValue(EntityAttributes_RangedWeapon.DAMAGE.attribute);
                var level = EnchantmentHelper.getLevel(Enchantments.POWER, query.entity().getMainHandStack());
                power *= 1 + (0.05 * level);
                // TODO: Maybe consider ranged weapon speed
                return power;
            });
        }
        SpellSchools.register(PHYSICAL_RANGED);

        initialized = true;
    }
}
