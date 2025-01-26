package net.spell_engine.utils;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    public static boolean matching(@Nullable String s1, @Nullable String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 != null && s2 != null) {
            return s1.equals(s2);
        }
        return false;
    }

    public static boolean matchesPattern(String subject, String regex) {
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
