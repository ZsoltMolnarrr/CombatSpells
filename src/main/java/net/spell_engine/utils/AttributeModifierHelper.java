package net.spell_engine.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;

public class AttributeModifierHelper {
    public static @NotNull Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifierMultimap(ItemStack itemStack) {
        var modifiers = itemStack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifiersMap = HashMultimap.create();
        for (var entry : modifiers.modifiers()) {
            modifiersMap.put(entry.attribute(), entry.modifier());
        }
        return modifiersMap;
    }

    public static @NotNull Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> fromModifier(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier) {
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifiersMap = HashMultimap.create();
        modifiersMap.put(attribute, modifier);
        return modifiersMap;
    }
}
