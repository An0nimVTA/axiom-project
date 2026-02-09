package com.axiom.ui;

import net.minecraft.network.chat.Component;

public final class UiText {
    private static UiLanguage language = UiLanguage.RU;
    private static boolean hasServerPreference = false;

    private UiText() {}

    public static void setLanguageCode(String code, boolean fromServer) {
        UiLanguage next = UiLanguage.fromCode(code);
        language = next;
        if (fromServer) {
            hasServerPreference = code != null && !code.isBlank();
        }
    }

    public static void setLanguage(UiLanguage lang, boolean fromServer) {
        language = lang == null ? UiLanguage.RU : lang;
        if (fromServer) {
            hasServerPreference = true;
        }
    }

    public static UiLanguage getLanguage() {
        return language;
    }

    public static boolean hasServerPreference() {
        return hasServerPreference;
    }

    public static String pick(String ru, String en) {
        if (language == UiLanguage.EN) {
            return en == null || en.isBlank() ? ru : en;
        }
        return ru;
    }

    public static Component text(String ru, String en) {
        return Component.literal(pick(ru, en));
    }
}
