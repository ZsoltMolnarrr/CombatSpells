package net.spell_engine.mixin.registry;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellRegistry_V2;
import net.spell_power.api.SpellSchools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Optional;

@Mixin(RegistryLoader.class)
public class RegistryLoaderMixin {
    @Inject(method = "loadFromResource(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/registry/RegistryOps$RegistryInfoGetter;Lnet/minecraft/registry/MutableRegistry;Lcom/mojang/serialization/Decoder;Ljava/util/Map;)V",
            at = @At("TAIL"))
    private static void spellEngine_RegistryFromResource(
            ResourceManager resourceManager, RegistryOps.RegistryInfoGetter infoGetter,
            MutableRegistry<?> registry, Decoder<?> elementDecoder, Map<RegistryKey<?>,
            Exception> errors, CallbackInfo ci) {

        // This is an attempt to programatically inject new entries into a DynamicRegistry
        // right after it's loaded from a resources.

        if (registry.getKey().equals(SpellRegistry_V2.KEY)) {
            var spellRegistry = (MutableRegistry<Spell>) registry;
            var namespace = "spell_engine";
            var path = "xyz";
            var version = "1";

            var newKey = RegistryKey.of(spellRegistry.getKey(), Identifier.of(namespace, path));
            var newSpell = new Spell();
            newSpell.school = SpellSchools.FIRE;
            newSpell.learn.tier = 7979;
            spellRegistry.add(newKey, newSpell,
                    new RegistryEntryInfo(Optional.empty(), registry.getLifecycle())
            );
//            spellRegistry.add(newKey, new Spell(),
//                    new RegistryEntryInfo(Optional.of(new VersionedIdentifier(namespace, path, version)), registry.getLifecycle())
//            );
//            spellRegistry.add(RegistryKey.ofRegistry()
//                    RegistryKey.of(SpellRegistry.KEY, Identifier.of("spell_engine:xyz"), ))
            System.out.println("Spell Engine: Loaded spell registry from resource " + registry.getKey().toString());
        }
    }
}
