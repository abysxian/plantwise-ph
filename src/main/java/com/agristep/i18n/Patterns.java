package com.agristep.i18n;

import java.util.List;

/**
 * Filipino patterns for sentences that carry numbers, dates or names. In a template {@code {1}} is the matched part
 * as written (a name, a number) and {@code {t1}} the same part translated.
 */
final class Patterns {
    private Patterns() {}

    private static final String MONTHS = "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)";

    static void addTo(List<Translator.Rule> r) {
        // ---- dates ----
        r.add(rule("(\\d{1,2}) " + MONTHS + "( \\d{4})?", "{1} {t2}{3}"));

        // ---- garden ----
        r.add(rule("Day (\\d+) of (\\d+)", "Araw {1} ng {2}"));
        r.add(rule("Day (\\d+)", "Araw {1}"));
        r.add(rule("day (\\d+) of about (\\d+)", "araw {1} ng humigit-kumulang {2}"));
        r.add(rule("Planted (.+), harvested (.+)", "Itinanim noong {t1}, inani noong {t2}"));
        r.add(rule("Planted (.+) in (.+)", "Itinanim ang {1} sa {2}"));
        r.add(rule("Planted (\\d{1,2} .+)", "Itinanim noong {t1}"));
        r.add(rule("(\\d+) days", "{1} araw"));
        r.add(rule("(\\d+) day", "{1} araw"));
        r.add(rule("About (\\d+) days", "Mga {1} araw"));
        r.add(rule("Ready in about (\\d+) days", "Handa sa loob ng mga {1} araw"));
        r.add(rule("(\\d+)% grown", "{1}% na ang laki"));
        r.add(rule("Overdue by (\\d+) days?", "Lampas na ng {1} araw"));
        r.add(rule("was due (.+)", "dapat sana ay {t1}"));
        r.add(rule("Due in (\\d+) days", "Due sa loob ng {1} araw"));
        r.add(rule("Due tomorrow", "Due bukas"));
        r.add(rule("Due today", "Due ngayon"));
        r.add(rule("Done (.+)", "Tapos noong {t1}"));
        r.add(rule("(.+) is empty again", "Walang tanim na ulit ang {1}"));
        r.add(rule("(.+) is empty", "Walang tanim ang {1}"));
        r.add(rule("(\\d+) × (\\d+) grid, (\\d+) beds?", "Grid na {1} × {2}, {3} taniman"));
        r.add(rule("(\\d+) × (\\d+) = (\\d+) beds?", "{1} × {2} = {3} taniman"));
        r.add(rule("Your garden is now (\\d+) × (\\d+) \\((\\d+) beds\\)", "Ang hardin mo ay {1} × {2} na ngayon ({3} taniman)"));
        r.add(rule("Each bed fills with green as its crop grows", "Napupuno ng berde ang bawat taniman habang lumalaki ang pananim"));
        r.add(rule("Tap a bed to see what to do next", "I-tap ang taniman para makita ang susunod na gagawin"));
        r.add(rule("Which bed for (.+)\\?", "Aling taniman para sa {1}?"));
        r.add(rule("Harvest (.+) from (.+) anyway\\?", "Anihin pa rin ang {1} mula sa {2}?"));
        r.add(rule("Harvested (.+) from (.+)", "Naani ang {1} mula sa {2}"));
        r.add(rule("Clear (.+)\\?", "Alisin ang tanim sa {1}?"));
        r.add(rule("The care plan for (.+) is removed and nothing is saved as a harvest", "Aalisin ang plano ng pag-aalaga para sa {1} at walang mase-save na ani"));
        r.add(rule("Good neighbours: (.+)", "Magandang magkatabi: {1}"));
        r.add(rule("Better apart: (.+)", "Mas mabuting magkahiwalay: {1}"));
        r.add(rule("Rotate crops: (.+)", "Magpalit-palit ng pananim: {1}"));

        // ---- planting hints ----
        r.add(rule("(.+) usually grows well next to (.+)", "Karaniwang tumutubo nang maayos ang {1} sa tabi ng {2}"));
        r.add(rule("(.+) and (.+) are often kept apart", "Madalas na pinaghihiwalay ang {1} at {2}"));
        r.add(rule("This bed grew (.+) (within the last month|about \\d+ months ago), the same plant family as (.+)",
                "Ang taniman na ito ay may tanim na {1} {t2}, kapamilya ng {3}"));
        r.add(rule("This bed grew (.+) (within the last month|about \\d+ months ago)", "Ang taniman na ito ay may tanim na {1} {t2}"));
        r.add(rule("about (\\d+) months ago", "mga {1} buwan na ang nakalipas"));

        // ---- weather and advice ----
        r.add(rule("Heavy rain: about (\\d+) mm expected", "Malakas na ulan: mga {1} mm ang inaasahan"));
        r.add(rule("Strong wind: gusts up to (\\d+) km/h", "Malakas na hangin: bugso hanggang {1} km/h"));
        r.add(rule("Extreme heat: up to (\\d+) °C", "Matinding init: hanggang {1} °C"));
        r.add(rule("(.+) \\((today|tomorrow|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)\\)", "{t1} ({t2})"));
        r.add(rule("Rain today \\(about (\\d+) mm\\)", "Uulan ngayon (mga {1} mm)"));
        r.add(rule("Hot day ahead, up to (\\d+) °C", "Mainit ngayong araw, hanggang {1} °C"));
        r.add(rule("(\\d+) tasks? (?:is|are) due in your garden", "{1} gawain ang due sa iyong hardin"));
        r.add(rule("1 task is due in your garden", "1 gawain ang due sa iyong hardin"));
        r.add(rule("1 bed is ready to harvest", "1 taniman ang handa nang anihin"));
        r.add(rule("(\\d+) beds are ready to harvest", "{1} taniman ang handa nang anihin"));
        r.add(rule("Rain is forecast \\(about (\\d+) mm in the next 2 days\\)", "May ulan sa forecast (mga {1} mm sa susunod na 2 araw)"));
        r.add(rule("Give each plant about ([\\d.]+) L of water\\.", "Bigyan ang bawat halaman ng mga {1} L ng tubig."));
        r.add(rule("Sow the seeds about ([\\d.]+) cm deep\\.", "Ihasik ang mga buto nang mga {1} cm ang lalim."));
        r.add(rule("Ready when (.+)", "Handa nang anihin kapag {t1}"));
        r.add(rule("Look for (.+)", "Hanapin ang {t1}"));
        r.add(rule("Safe remedy: (.+)", "Ligtas na lunas: {t1}"));
        r.add(rule("(\\d+) garden tasks are due today or overdue", "{1} gawain sa hardin ang due ngayon o lampas na"));

        // ---- why a crop fits ----
        r.add(rule("Recent average temperature is ([\\d.]+ °C), right in its ideal range \\((.+)\\)",
                "Ang kamakailang karaniwang temperatura ay {1}, nasa mainam na saklaw nito ({2})"));
        r.add(rule("Recent average temperature is ([\\d.]+ °C), outside what it survives \\((.+)\\)",
                "Ang kamakailang karaniwang temperatura ay {1}, lampas sa kaya nitong tiisin ({2})"));
        r.add(rule("Recent average temperature is ([\\d.]+ °C)", "Ang kamakailang karaniwang temperatura ay {1}"));
        r.add(rule("It will grow, but it prefers (.+)", "Tutubo ito, pero mas gusto nito ang {1}"));
        r.add(rule("Rain over the past year was ([\\d,]+ mm), inside its ideal (.+)", "Ang ulan sa nakaraang taon ay {1}, nasa mainam nitong {2}"));
        r.add(rule("Rain over the past year was ([\\d,]+ mm) \\(ideal is (.+)\\)", "Ang ulan sa nakaraang taon ay {1} (ang mainam ay {2})"));
        r.add(rule("Rain over the past year was ([\\d,]+ mm), outside what it tolerates", "Ang ulan sa nakaraang taon ay {1}, lampas sa kaya nitong tiisin"));
        r.add(rule("About ([\\d.]+) °C and (dry|moderately wet|wet) \\(around (\\d+) mm of rain\\)",
                "Mga {1} °C at {t2} (mga {3} mm na ulan)"));
        r.add(rule("moderately wet", "katamtamang basa"));
        r.add(rule("wet", "basa"));
        r.add(rule("(.+) is ideal", "Mainam ang {1}"));
        r.add(rule("(.+) a year", "{1} sa isang taon"));
        r.add(rule("Soil: (.+)", "Lupa: {t1}"));
        r.add(rule("(.+) Soil: (.+)", "{t1} Lupa: {t2}"));

        // ---- screens ----
        r.add(rule("Welcome, (.+)!", "Maligayang pagdating, {1}!"));
        r.add(rule("PlantWise PH - (.+)", "PlantWise PH - {1}"));
        r.add(rule("TODAY IN (.+)", "NGAYON SA {1}"));
        r.add(rule("Checking what fits (.+)", "Sinusuri kung ano ang bagay sa {1}"));
        r.add(rule("Ranked for (.+) with (.+)", "Niraranggo para sa {1} na may {t2}"));
        r.add(rule("Soil set to (.+)", "Itinakda ang lupa sa {t1}"));
        r.add(rule("(.+) · humidity (\\d+)%", "{t1} · halumigmig {2}%"));
        r.add(rule("humidity (\\d+)%", "halumigmig {1}%"));
        r.add(rule("…and (\\d+) more in your garden", "…at {1} pa sa iyong hardin"));
        r.add(rule("Show all (\\d+) crops", "Ipakita ang lahat ng {1} pananim"));
        r.add(rule("Which vegetables suit each month at (.+), worked out from the last year of weather there\\.",
                "Aling mga gulay ang bagay sa bawat buwan sa {1}, batay sa panahon doon sa nakaraang taon."));
        r.add(rule("(\\d+) vegetables for Philippine gardens", "{1} gulay para sa mga hardin sa Pilipinas"));
        r.add(rule("(\\d+) of (\\d+) match \"(.+)\"\\.", "{1} sa {2} ang tumutugma sa \"{3}\"."));
        r.add(rule("(\\d+) harvests? recorded", "{1} ani ang naitala"));
        r.add(rule("(\\d+) notes? so far\\. Newest first\\.", "{1} tala na sa ngayon. Pinakabago muna."));
        r.add(rule("Written by (.+)", "Isinulat ni {1}"));
        r.add(rule("Photo: (.+)", "Litrato: {1}"));
        r.add(rule("(.+) \\(empty\\)", "{1} (walang tanim)"));
        r.add(rule("Saved (.+)\\.", "Nai-save ang {1}."));
        r.add(rule("Location set to (.+)\\.", "Itinakda ang lokasyon sa {1}."));
        r.add(rule("Your garden is at (.+) \\((.+)\\)", "Ang iyong hardin ay nasa {1} ({2})"));
        r.add(rule("(Latitude|Longitude) must be a number between -(\\d+) and (\\d+)\\.", "Ang {t1} ay dapat numero mula -{2} hanggang {3}."));
        r.add(rule("Search isn't available right now \\((.*)\\)", "Hindi available ang paghahanap ngayon ({1})"));
        r.add(rule("No place found for \"(.+)\"\\. Try another spelling\\.", "Walang nahanap na lugar para sa \"{1}\". Subukan ang ibang baybay."));
        r.add(rule("Username: (\\S+)\\s+Password: (\\S+)", "Username: {1}   Password: {2}"));
        r.add(rule("(-?\\d+) to (-?\\d+), e\\.g\\. ([\\d.]+)", "{1} hanggang {2}, hal. {3}"));
        r.add(rule("Something went wrong: (.+)", "May nangyaring mali: {t1}"));
        r.add(rule("Too many wrong attempts\\. Try again in (\\d+) seconds\\.", "Napakaraming maling subok. Subukan muli sa loob ng {1} segundo."));
        r.add(rule("Rows must be a whole number from 1 to (\\d+)\\.", "Ang mga hanay ay dapat buong numero mula 1 hanggang {1}."));
        r.add(rule("Columns must be a whole number from 1 to (\\d+)\\.", "Ang mga kolum ay dapat buong numero mula 1 hanggang {1}."));
        r.add(rule("The note is too long \\((\\d+) letters at most\\)\\.", "Masyadong mahaba ang tala (hanggang {1} titik lang)."));
        r.add(rule("(.+) is already in your garden\\.", "Nasa hardin mo na si {1}."));
        r.add(rule("You already invited (.+)\\.", "Inimbitahan mo na si {1}."));
        r.add(rule("(.+) is empty, so there is nothing to harvest\\.", "Walang tanim ang {1}, kaya walang aanihin."));
        r.add(rule("Open this address in your browser: (.+)", "Buksan ang address na ito sa iyong browser: {1}"));
        r.add(rule("Couldn't open the browser\\. Address: (.+)", "Hindi mabuksan ang browser. Address: {1}"));

        // ---- shared garden ----
        r.add(rule("You are working in (.+)'s garden", "Nagtatrabaho ka sa hardin ni {1}"));
        r.add(rule("(.+) is no longer in your garden\\.", "Wala na si {1} sa iyong hardin."));
        r.add(rule("You joined (.+)'s garden\\.", "Sumali ka sa hardin ni {1}."));
        r.add(rule("(.+) invited you to their garden", "Inimbitahan ka ni {1} sa kanyang hardin"));
        r.add(rule("Invitation sent to (.+)", "Naipadala ang imbitasyon kay {1}"));
        r.add(rule("Leave (.+)'s garden\\?", "Umalis sa hardin ni {1}?"));

        // ---- care plan shown on the detail screen ----
        r.add(rule("Fewer (.+)", "Bawasan ang {1}"));
        r.add(rule("More (.+)", "Dagdagan ang {1}"));
    }

    private static Translator.Rule rule(String regex, String template) { return new Translator.Rule(regex, template); }
}
