package com.agristep.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the app's English text into Filipino. The app is written in English; this class is the one place that knows
 * the Filipino versions, so screens, services and the crop guides all translate the same way.
 *
 * <p>How a text is translated, in order: (1) an exact match in the word lists ({@code /i18n/*.tsv}); (2) pieces split at
 * " · " and at sentence ends, each translated on its own, longest run of sentences first; (3) a pattern for sentences
 * that carry numbers or names, such as "Due in 3 days"; (4) the same text with its final full stop or colon removed.
 * Text with no Filipino version (a person's own notes, place names) is returned unchanged.
 */
public final class Translator {
    private Translator() {}

    private static volatile boolean filipino = false;

    private static final Map<String, String> EXACT = new HashMap<>();
    private static final List<Rule> RULES = new ArrayList<>();
    /** Beds are saved as "Bed 1", "Bed 2" and so on; they show as "Taniman 1" in Filipino. */
    private static final Pattern BED = Pattern.compile("\\bBed (\\d+)\\b");
    private static final Pattern LABEL = Pattern.compile("(Good neighbours|Better apart|Rotate crops): (.*)", Pattern.DOTALL);
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])(?<!e\\.g\\.)\\s+");

    static {
        for (String file : new String[]{"fil-ui.tsv", "fil-crops.tsv"}) load("/i18n/" + file);
        Patterns.addTo(RULES);
    }

    public static boolean isFilipino() { return filipino; }

    public static void setFilipino(boolean value) { filipino = value; }

    /** The locale for formatting dates and numbers in the chosen language. */
    public static Locale locale() { return filipino ? Locale.forLanguageTag("fil-PH") : Locale.ENGLISH; }

    /** Adds more exact translations (used for the short lists kept in code). */
    public static void register(Map<String, String> more) { EXACT.putAll(more); }

    /** The Filipino text when the app is in Filipino and a translation exists; otherwise the text as given. */
    public static String t(String english) {
        if (!filipino || english == null || english.isBlank()) return english;
        return BED.matcher(translate(english, 0)).replaceAll("Taniman $1");
    }

    // ---- the translation steps ----

    private static String translate(String s, int depth) {
        if (depth > 6) return s;
        String exact = EXACT.get(s);
        if (exact != null) return exact;

        // lines (the Today screen joins reasons with newlines)
        if (s.indexOf('\n') >= 0) {
            String[] lines = s.split("\n", -1);
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) out.append('\n');
                out.append(translate(lines[i], depth + 1));
            }
            return out.toString();
        }

        // a list item
        if (s.startsWith("• ")) return "• " + translate(s.substring(2), depth + 1);

        // a label in front of a sentence, e.g. "Good neighbours: ..."
        Matcher label = LABEL.matcher(s);
        if (label.matches()) return translate(label.group(1), depth + 1) + ": " + translate(label.group(2), depth + 1);

        // pieces joined with " · "
        if (s.contains(" · ")) {
            String[] parts = s.split(" · ", -1);
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) out.append(" · ");
                out.append(translate(parts[i], depth + 1));
            }
            return out.toString();
        }

        // several sentences: translate the longest run that has a translation, then carry on
        String[] sentences = SENTENCE_END.split(s);
        if (sentences.length > 1) {
            StringBuilder out = new StringBuilder();
            int i = 0;
            while (i < sentences.length) {
                String best = null;
                int bestEnd = i;
                for (int j = sentences.length - 1; j >= i; j--) {
                    String run = String.join(" ", java.util.Arrays.copyOfRange(sentences, i, j + 1));
                    String found = EXACT.get(run);
                    if (found != null) { best = found; bestEnd = j; break; }
                }
                if (best == null) {
                    best = translate(sentences[i], depth + 1);
                    bestEnd = i;
                }
                if (out.length() > 0) out.append(' ');
                out.append(best);
                i = bestEnd + 1;
            }
            return out.toString();
        }

        // a single sentence: patterns, then the text without its closing punctuation
        String byRule = applyRules(s, depth);
        if (byRule != null) return byRule;

        String bare = stripEnd(s);
        if (!bare.equals(s)) {
            String tail = s.substring(bare.length());
            String again = EXACT.get(bare);
            if (again == null) again = applyRules(bare, depth);
            if (again != null) return again + tail;
        } else {
            String withStop = EXACT.get(s + ".");
            if (withStop != null) return stripEnd(withStop);
        }
        return s;
    }

    private static String stripEnd(String s) {
        int end = s.length();
        while (end > 0 && ".!?:…".indexOf(s.charAt(end - 1)) >= 0 && end > 1) end--;
        return s.substring(0, end);
    }

    private static String applyRules(String s, int depth) {
        for (Rule rule : RULES) {
            Matcher m = rule.pattern.matcher(s);
            if (m.matches()) return rule.fill(m, depth);
        }
        return null;
    }

    /** A pattern for a sentence with numbers or names in it. {@code {1}} is group 1 as written, {@code {t1}} translated. */
    static final class Rule {
        private static final Pattern SLOT = Pattern.compile("\\{(t?)(\\d)}");
        final Pattern pattern;
        final String template;

        Rule(String regex, String template) {
            this.pattern = Pattern.compile(regex, Pattern.DOTALL);
            this.template = template;
        }

        String fill(Matcher m, int depth) {
            Matcher slot = SLOT.matcher(template);
            StringBuilder out = new StringBuilder();
            while (slot.find()) {
                String value = m.group(Integer.parseInt(slot.group(2)));
                if (value == null) value = "";
                if (!slot.group(1).isEmpty()) value = translate(value, depth + 1);
                slot.appendReplacement(out, Matcher.quoteReplacement(value));
            }
            slot.appendTail(out);
            return out.toString();
        }
    }

    private static void load(String resource) {
        try (InputStream in = Translator.class.getResourceAsStream(resource)) {
            if (in == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int tab = line.indexOf('\t');
                if (tab > 0) EXACT.put(line.substring(0, tab), line.substring(tab + 1));
            }
        } catch (IOException e) {
            // a missing word list only means more English text; never stop the app for it
        }
    }
}
