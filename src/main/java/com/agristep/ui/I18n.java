package com.agristep.ui;

import java.util.Map;

/**
 * English and Filipino for the app's menus, screen titles and main buttons. Anything without a Filipino entry
 * (longer explanations, tips, error messages) is shown in English. English is the default.
 */
public final class I18n {
    private I18n() {}

    public enum Language {
        ENGLISH("English", "en"), FILIPINO("Filipino", "fil");

        private final String label;
        private final String code;

        Language(String label, String code) {
            this.label = label;
            this.code = code;
        }

        public String label() { return label; }
        public String code() { return code; }

        public static Language fromCode(String code) {
            for (Language l : values()) if (l.code.equalsIgnoreCase(code == null ? "" : code.trim())) return l;
            return ENGLISH;
        }
    }

    private static volatile Language current = Language.ENGLISH;

    public static Language current() { return current; }

    public static void use(Language language) { current = language == null ? Language.ENGLISH : language; }

    /** The text in the chosen language, or the English text when there is no translation. */
    public static String t(String english) {
        if (current == Language.ENGLISH || english == null) return english;
        return FILIPINO.getOrDefault(english, english);
    }

    private static final Map<String, String> FILIPINO = Map.ofEntries(
            // menu
            Map.entry("Today", "Ngayon"), Map.entry("Garden", "Hardin"), Map.entry("Crops", "Mga Pananim"),
            Map.entry("Calendar", "Kalendaryo"), Map.entry("Journal", "Talaarawan"), Map.entry("Insights", "Estadistika"),
            Map.entry("Location", "Lokasyon"), Map.entry("Account", "Account"),
            Map.entry("Sign out", "Mag-sign out"), Map.entry("Signed in as", "Naka-sign in bilang"),
            Map.entry("Backyard farming", "Pagtatanim sa bakuran"),
            // screen titles
            Map.entry("Your garden", "Ang iyong hardin"), Map.entry("What to plant", "Ano ang itatanim"),
            Map.entry("Planting calendar", "Kalendaryo ng pagtatanim"), Map.entry("Garden journal", "Talaarawan ng hardin"),
            Map.entry("Your harvests", "Ang iyong mga ani"), Map.entry("Your account", "Ang iyong account"),
            Map.entry("Checklist", "Listahan ng gagawin"), Map.entry("Harvest history", "Kasaysayan ng ani"),
            Map.entry("Grid size", "Laki ng grid"), Map.entry("Coming up", "Paparating"),
            Map.entry("Weather alerts", "Babala sa panahon"), Map.entry("Shared garden", "Pinagsasaluhang hardin"),
            Map.entry("Change password", "Palitan ang password"), Map.entry("Language", "Wika"),
            Map.entry("Delete account", "Burahin ang account"),
            Map.entry("What soil is in your garden?", "Anong lupa ang nasa hardin mo?"),
            // buttons
            Map.entry("Plant", "Itanim"), Map.entry("Plant here", "Itanim dito"), Map.entry("Harvest", "Anihin"),
            Map.entry("Clear bed", "Alisin ang tanim"), Map.entry("Care guide", "Gabay sa pag-aalaga"),
            Map.entry("Edit grid", "Baguhin ang grid"), Map.entry("Done", "Tapos na"), Map.entry("Apply grid", "Ilapat ang grid"),
            Map.entry("Change place", "Palitan ang lugar"), Map.entry("Search", "Hanapin"),
            Map.entry("Save location", "I-save ang lokasyon"), Map.entry("Sign in", "Mag-sign in"),
            Map.entry("Create account", "Gumawa ng account"), Map.entry("Save entry", "I-save ang tala"),
            Map.entry("Add photo", "Magdagdag ng litrato"), Map.entry("Delete", "Burahin"), Map.entry("Cancel", "Kanselahin"),
            Map.entry("Accept", "Tanggapin"), Map.entry("Decline", "Tanggihan"), Map.entry("Invite", "Imbitahan"),
            Map.entry("Remove", "Alisin"), Map.entry("Leave garden", "Umalis sa hardin"),
            Map.entry("Write a note", "Sumulat ng tala"), Map.entry("Show fewer", "Magpakita ng kaunti"),
            Map.entry("What fits best?", "Mga mungkahi"),
            Map.entry("Export harvests (CSV)", "I-export ang ani (CSV)"), Map.entry("Export care plan (CSV)", "I-export ang plano ng pag-aalaga (CSV)"),
            Map.entry("Garden report (PDF)", "Ulat ng hardin (PDF)"),
            // greetings
            Map.entry("Good morning", "Magandang umaga"), Map.entry("Good afternoon", "Magandang hapon"),
            Map.entry("Good evening", "Magandang gabi"));
}
