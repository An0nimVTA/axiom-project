package com.axiom.ui;

public enum UiLanguage {
    RU("ru", "Русский", "Russian"),
    EN("en", "English", "English");

    private final String code;
    private final String nativeName;
    private final String englishName;

    UiLanguage(String code, String nativeName, String englishName) {
        this.code = code;
        this.nativeName = nativeName;
        this.englishName = englishName;
    }

    public String getCode() {
        return code;
    }

    public String getNativeName() {
        return nativeName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public static UiLanguage fromCode(String code) {
        if (code == null || code.isBlank()) {
            return RU;
        }
        String normalized = code.trim().toLowerCase();
        for (UiLanguage lang : values()) {
            if (lang.code.equals(normalized)) {
                return lang;
            }
        }
        return RU;
    }
}
