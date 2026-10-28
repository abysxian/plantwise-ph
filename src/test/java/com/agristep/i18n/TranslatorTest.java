package com.agristep.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TranslatorTest {
    @AfterEach void backToEnglish() { Translator.setFilipino(false); }

    @Test void englishIsLeftAlone() {
        assertEquals("Due in 3 days", Translator.t("Due in 3 days"));
        assertNull(Translator.t(null));
    }

    @Test void exactSentencesAndNumbersAreTranslated() {
        Translator.setFilipino(true);
        assertEquals("Hardin", Translator.t("Garden"));
        assertEquals("Due sa loob ng 3 araw", Translator.t("Due in 3 days"));
        assertEquals("Lampas na ng 2 araw · dapat sana ay 3 Okt", Translator.t("Overdue by 2 days · was due 3 Oct"));
        assertEquals("Itinanim ang Talong sa Taniman 2.", Translator.t("Planted Talong in Bed 2."));
    }

    @Test void sentencesInARowAreTranslatedOneByOneOrTogether() {
        Translator.setFilipino(true);
        String text = Translator.t("Give each plant about 0.3 L of water. Water lightly every morning. The soil should feel damp, not soggy.");
        assertEquals("Bigyan ang bawat halaman ng mga 0.3 L ng tubig. Diligan nang bahagya tuwing umaga. "
                + "Dapat mamasa-masa ang lupa, hindi sobrang basa.", text);
    }

    @Test void careStepsKeepTheirFramingWords() {
        Translator.setFilipino(true);
        assertEquals("Handa nang anihin kapag makintab at matigas ang mga bunga, at bumabalik ang balat kapag bahagyang piniga. "
                        + "Gupitin gamit ang gunting at mag-iwan ng maikling piraso ng tangkay. Madalas na mamitas para tuloy-tuloy ang bunga.",
                Translator.t("Ready when the fruits are shiny and firm, and the skin springs back when pressed lightly. "
                        + "Cut with scissors and leave a short piece of stem. Pick often so the plant keeps producing."));
    }

    @Test void namesAndUnknownTextStayAsTheyAre() {
        Translator.setFilipino(true);
        assertEquals("Leaves look healthy today", Translator.t("Leaves look healthy today"));
        assertEquals("Talong · Eggplant", Translator.t("Talong · Eggplant"));
    }

    @Test void everyWordListEntryHasAFilipinoText() {
        Translator.setFilipino(true);
        assertNotEquals("Water the plants", Translator.t("Water the plants"));
        assertNotEquals("Thunderstorm in the forecast (Monday)", Translator.t("Thunderstorm in the forecast (Monday)"));
    }

    @Test void datesAndMonthsUseFilipinoNames() {
        Translator.setFilipino(true);
        assertEquals("28 Okt 2026", Translator.t("28 Oct 2026"));
        assertEquals("Disyembre", Translator.t("December"));
    }
}
