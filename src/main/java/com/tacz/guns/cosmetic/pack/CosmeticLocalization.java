package com.tacz.guns.cosmetic.pack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CosmeticLocalization {
    private static final Map<String, Map<String, String>> LANG = new HashMap<>();

    private CosmeticLocalization() {}

    public static void clear() {
        LANG.clear();
    }

    public static void add(String locale, Map<String, String> entries) {
        if (locale == null || entries.isEmpty()) return;
        LANG.computeIfAbsent(locale.toLowerCase(Locale.ROOT), k -> new HashMap<>()).putAll(entries);
    }

    public static String translate(String key, String locale) {
        if (key == null || key.isEmpty()) return "";
        String normalized = locale == null ? "en_us" : locale.toLowerCase(Locale.ROOT);
        String translated = lookup(normalized, key);
        if (translated != null) return translated;
        translated = lookup("en_us", key);
        return translated != null ? translated : key;
    }

    private static String lookup(String locale, String key) {
        Map<String, String> entries = LANG.get(locale);
        return entries != null ? entries.get(key) : null;
    }
}
