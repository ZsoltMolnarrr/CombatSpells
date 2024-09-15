package net.spell_engine.utils;

import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.SpellContainer;
import net.spell_engine.internals.SpellRegistry;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeaponCompatibility {
    public static void initialize() {
        var config = SpellEngineMod.config;
        var spellProxyContainer = new SpellContainer(SpellContainer.ContentType.MAGIC, true, null, null, 0, List.of());
        var arrowProxyContainer = new SpellContainer(SpellContainer.ContentType.ARCHERY, true, null, null, 0, List.of());
        for(var itemId: Registries.ITEM.getIds()) {
            var itemIdString = itemId.toString();
            if (matches(itemIdString, config.blacklist_spell_casting_regex)) {
                continue;
            }
            var item = Registries.ITEM.get(itemId);
            boolean addProxy = false;
            var contentType = SpellContainer.ContentType.MAGIC;
            if (config.add_spell_casting_to_swords && item instanceof SwordItem) {
                addProxy = true;
            } else if (item instanceof RangedWeaponItem) {
                contentType = SpellContainer.ContentType.ARCHERY;
                addProxy = true;
            } else if (matches(itemIdString, config.add_spell_casting_regex)) {
                addProxy = true;
            }
            if (addProxy) {
                switch (contentType) {
                    case MAGIC -> {
                        SpellRegistry.containers.putIfAbsent(itemId, spellProxyContainer);
                    }
                    case ARCHERY -> {
                        SpellRegistry.containers.putIfAbsent(itemId, arrowProxyContainer);
                    }
                }
            }
        }
    }

    public static boolean matches(String subject, String nullableRegex) {
        if (subject == null) {
            return false;
        }
        if (nullableRegex == null || nullableRegex.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile(nullableRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(subject);
        return matcher.find();
    }
}
