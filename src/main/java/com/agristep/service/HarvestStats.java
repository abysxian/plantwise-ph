package com.agristep.service;

import com.agristep.domain.HarvestRecord;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Numbers and chart data worked out from the harvest history. */
public final class HarvestStats {
    private HarvestStats() {}

    public record CropCount(String cropName, String emoji, int count) {}

    public record Summary(int total, double averageDaysToHarvest, int averageCompletion,
                          List<CropCount> byCrop, Map<YearMonth, Integer> perMonth) {
        public String topCrop() { return byCrop.isEmpty() ? null : byCrop.get(0).cropName(); }
    }

    /** @param months how many months to chart, ending with the month of {@code today} */
    public static Summary summarise(List<HarvestRecord> records, LocalDate today, int months) {
        Map<YearMonth, Integer> perMonth = new LinkedHashMap<>();
        YearMonth current = YearMonth.from(today);
        for (int i = months - 1; i >= 0; i--) perMonth.put(current.minusMonths(i), 0);

        Map<String, int[]> counts = new TreeMap<>();
        Map<String, String> emojis = new TreeMap<>();
        double days = 0;
        double completion = 0;
        for (HarvestRecord r : records) {
            perMonth.computeIfPresent(YearMonth.from(r.getHarvestedOn()), (k, v) -> v + 1);
            counts.computeIfAbsent(r.getCropName(), k -> new int[1])[0]++;
            emojis.putIfAbsent(r.getCropName(), r.getCropEmoji());
            days += r.daysGrown();
            completion += r.getCompletionPercent();
        }
        List<CropCount> byCrop = new ArrayList<>();
        counts.forEach((name, c) -> byCrop.add(new CropCount(name, emojis.get(name), c[0])));
        byCrop.sort(Comparator.comparingInt(CropCount::count).reversed().thenComparing(CropCount::cropName));
        int n = records.size();
        return new Summary(n, n == 0 ? 0 : days / n, n == 0 ? 0 : (int) Math.round(completion / n), byCrop, perMonth);
    }
}
