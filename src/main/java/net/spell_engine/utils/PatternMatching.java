package net.spell_engine.utils;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternMatching {
    public static final String TAG_PREFIX = "#";
    public static final String REGEX_PREFIX = "~";

    public static <T> boolean matches(RegistryEntry<T> entry, RegistryKey<Registry<T>> registryKey, String pattern) {
        if (pattern.startsWith(TAG_PREFIX)) {
            var tag = TagKey.of(registryKey, Identifier.of(pattern.substring(1)));
            return entry.isIn(tag);
        }
        var id = entry.getKey().get().getValue().toString();
        if (pattern.startsWith(REGEX_PREFIX)) {
            return regexMatches(id, pattern.substring(1));
        } else {
            return id.equals(pattern);
        }
    }

    public static boolean regexMatches(String subject, String regex) {
        if (subject == null) {
            return false;
        }
        if (regex == null || regex.isEmpty() || regex.equals("*")) {
            return true;
        }
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(subject);
        return matcher.find();
    }
}
