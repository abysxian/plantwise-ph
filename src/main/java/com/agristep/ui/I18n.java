package com.agristep.ui;

import com.agristep.i18n.Translator;

/**
 * The app's language switch, English or Filipino. English is the default. The Filipino wording itself lives in
 * {@link Translator} and its word lists under {@code /i18n}, so screens, services and crop guides share it.
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

    public static void use(Language language) {
        current = language == null ? Language.ENGLISH : language;
        Translator.setFilipino(current == Language.FILIPINO);
        // the standard dialog and file-chooser buttons
        boolean fil = current == Language.FILIPINO;
        javax.swing.UIManager.put("OptionPane.okButtonText", fil ? "OK" : "OK");
        javax.swing.UIManager.put("OptionPane.cancelButtonText", fil ? "Kanselahin" : "Cancel");
        javax.swing.UIManager.put("OptionPane.yesButtonText", fil ? "Oo" : "Yes");
        javax.swing.UIManager.put("OptionPane.noButtonText", fil ? "Hindi" : "No");
        javax.swing.UIManager.put("FileChooser.saveButtonText", fil ? "I-save" : "Save");
        javax.swing.UIManager.put("FileChooser.openButtonText", fil ? "Buksan" : "Open");
        javax.swing.UIManager.put("FileChooser.cancelButtonText", fil ? "Kanselahin" : "Cancel");
    }

    /** The text in the chosen language, or the English text when there is no translation. */
    public static String t(String english) { return Translator.t(english); }
}
