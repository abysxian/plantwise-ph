package com.agristep.service;

import java.util.Map;
import java.util.Set;

/**
 * Plant families and which families are commonly said to grow well, or badly, side by side. This is traditional
 * gardening guidance rather than laboratory-tested fact, so the app words it as a suggestion.
 */
final class CropRelations {
    private CropRelations() {}

    static final Map<String, String> FAMILY = Map.ofEntries(
            Map.entry("talong", "Solanaceae (nightshade)"), Map.entry("kamatis", "Solanaceae (nightshade)"),
            Map.entry("sili", "Solanaceae (nightshade)"), Map.entry("siling-haba", "Solanaceae (nightshade)"),
            Map.entry("ampalaya", "Cucurbitaceae (gourd)"), Map.entry("pipino", "Cucurbitaceae (gourd)"),
            Map.entry("upo", "Cucurbitaceae (gourd)"), Map.entry("patola", "Cucurbitaceae (gourd)"),
            Map.entry("kalabasa", "Cucurbitaceae (gourd)"), Map.entry("kundol", "Cucurbitaceae (gourd)"),
            Map.entry("sitao", "Fabaceae (legume)"), Map.entry("sigarilyas", "Fabaceae (legume)"),
            Map.entry("sitsaro", "Fabaceae (legume)"), Map.entry("bataw", "Fabaceae (legume)"),
            Map.entry("baguio", "Fabaceae (legume)"),
            Map.entry("pechay", "Brassicaceae (cabbage)"), Map.entry("mustasa", "Brassicaceae (cabbage)"),
            Map.entry("labanos", "Brassicaceae (cabbage)"), Map.entry("repolyo", "Brassicaceae (cabbage)"),
            Map.entry("broccoli", "Brassicaceae (cabbage)"), Map.entry("kuliplor", "Brassicaceae (cabbage)"),
            Map.entry("wombok", "Brassicaceae (cabbage)"),
            Map.entry("sibuyas", "Amaryllidaceae (onion)"), Map.entry("bawang", "Amaryllidaceae (onion)"),
            Map.entry("carrot", "Apiaceae (carrot)"), Map.entry("kinchay", "Apiaceae (carrot)"),
            Map.entry("letsugas", "Asteraceae (lettuce)"),
            Map.entry("mais", "Poaceae (grass)"),
            Map.entry("kangkong", "Convolvulaceae (morning glory)"), Map.entry("kamote", "Convolvulaceae (morning glory)"),
            Map.entry("okra", "Malvaceae (mallow)"), Map.entry("saluyot", "Malvaceae (mallow)"),
            Map.entry("alugbati", "Basellaceae (vine spinach)"),
            Map.entry("spinach", "Amaranthaceae (amaranth)"),
            Map.entry("gabi", "Araceae (taro)"),
            Map.entry("luya", "Zingiberaceae (ginger)"));

    private static final Set<Set<String>> GOOD = Set.of(
            pair("Solanaceae (nightshade)", "Amaryllidaceae (onion)"),
            pair("Solanaceae (nightshade)", "Apiaceae (carrot)"),
            pair("Solanaceae (nightshade)", "Asteraceae (lettuce)"),
            pair("Poaceae (grass)", "Fabaceae (legume)"),
            pair("Poaceae (grass)", "Cucurbitaceae (gourd)"),
            pair("Cucurbitaceae (gourd)", "Fabaceae (legume)"),
            pair("Brassicaceae (cabbage)", "Amaryllidaceae (onion)"),
            pair("Brassicaceae (cabbage)", "Apiaceae (carrot)"),
            pair("Apiaceae (carrot)", "Amaryllidaceae (onion)"),
            pair("Apiaceae (carrot)", "Asteraceae (lettuce)"));

    private static final Set<Set<String>> AVOID = Set.of(
            pair("Solanaceae (nightshade)", "Brassicaceae (cabbage)"),
            pair("Solanaceae (nightshade)", "Poaceae (grass)"),
            pair("Fabaceae (legume)", "Amaryllidaceae (onion)"));

    /** Two families in either order; both may be the same family (for example two nightshades side by side). */
    private static Set<String> pair(String a, String b) { return new java.util.HashSet<>(java.util.Arrays.asList(a, b)); }

    static String familyOf(String code) { return FAMILY.getOrDefault(code, null); }

    static boolean goodTogether(String familyA, String familyB) { return GOOD.contains(pair(familyA, familyB)); }

    static boolean badTogether(String familyA, String familyB) { return AVOID.contains(pair(familyA, familyB)); }
}
