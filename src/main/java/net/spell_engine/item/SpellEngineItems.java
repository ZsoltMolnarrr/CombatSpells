package net.spell_engine.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.spellbinding.SpellBinding;
import net.spell_engine.spellbinding.SpellBindingBlock;

import java.util.Comparator;

public class SpellEngineItems {
    public class Group {
        public static Identifier ID = Identifier.of(SpellEngineMod.ID, "generic");
        public static RegistryKey<ItemGroup> KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), ID);
        public static ItemGroup SPELLS = FabricItemGroup.builder()
                .icon(() -> new ItemStack(SpellBindingBlock.ITEM))
                .displayName(Text.translatable("itemGroup." + SpellEngineMod.ID + ".general"))
                .build();
    }

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, Group.KEY, Group.SPELLS);
        Registry.register(Registries.ITEM, SpellBinding.ID, SpellBindingBlock.ITEM);
        Registry.register(Registries.ITEM, ScrollItem.ID, ScrollItem.ITEM);
        ItemGroupEvents.modifyEntriesEvent(Group.KEY).register(content -> {
            content.add(SpellBindingBlock.ITEM);
            SpellRegistry.all()
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(a -> a.getKey().getNamespace() + "_" + a.getValue().spell.learn.tier + "_" + a.getKey().getPath()))
                    .forEach((entry) -> {
                var scroll = ScrollItem.forSpell(entry.getKey(), entry.getValue().spell);
                if (scroll != null) {
                    content.add(scroll);
                }
            });
        });
    }
}
