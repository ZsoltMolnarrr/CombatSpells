package net.spell_engine;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.spell_engine.api.item.weapon.StaffItem;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.config.ServerConfig;
import net.spell_engine.config.ServerConfigWrapper;
import net.spell_engine.internals.SpellAssignments;
import net.spell_engine.internals.criteria.EnchantmentSpecificCriteria;
import net.spell_engine.network.ServerNetwork;
import net.spell_engine.particle.Particles;
import net.spell_engine.rpg_series.RPGSeriesCore;
import net.spell_engine.spellbinding.*;

import java.util.Set;

public class SpellEngineMod {
    public static final String ID = "spell_engine";
    public static String modName() {
        return I18n.translate("spell_engine.mod_name");
    }

    public static ServerConfig config;

    public static void init() {
        AutoConfig.register(ServerConfigWrapper.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));
        config = AutoConfig.getConfigHolder(ServerConfigWrapper.class).getConfig().server;

        DynamicRegistries.registerSynced(SpellRegistry.KEY, SpellRegistry.LOCAL_CODEC, SpellRegistry.NETWORK_CODEC);

        SpellAssignments.initialize();
        ServerNetwork.initializeHandlers();
        Particles.register();

        Criteria.register(EnchantmentSpecificCriteria.ID.toString(), EnchantmentSpecificCriteria.INSTANCE);

        var staffEnchantments = Set.of(Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT, Enchantments.LOOTING);
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, enchantingContext) -> {
            if (target.getItem() instanceof StaffItem && staffEnchantments.contains(enchantment.getKey().get())) {
                return TriState.TRUE;
            }
            return TriState.DEFAULT;
        });

        ExternalSpellSchools.initialize();
        RPGSeriesCore.initialize();
    }

    public static void registerSpellBinding() {
        Registry.register(Registries.BLOCK, SpellBinding.ID, SpellBindingBlock.INSTANCE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, SpellBinding.ID, SpellBindingBlockEntity.ENTITY_TYPE);
        Registry.register(Registries.SCREEN_HANDLER, SpellBinding.ID, SpellBindingScreenHandler.HANDLER_TYPE);
        Criteria.register(SpellBindingCriteria.ID.toString(), SpellBindingCriteria.INSTANCE);
        Criteria.register(SpellBookCreationCriteria.ID.toString(), SpellBookCreationCriteria.INSTANCE);
        Registry.register(Registries.LOOT_FUNCTION_TYPE, SpellBindRandomlyLootFunction.ID, SpellBindRandomlyLootFunction.TYPE);
    }
}