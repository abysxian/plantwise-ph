package com.agristep.domain;

import com.agristep.weather.WeatherForecast;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

public class WateringTask extends CareTask {
    public WateringTask(int dayOffset, LocalDate plantedOn, Double amount, String detail, String extra) {
        super(dayOffset, plantedOn, amount, detail, extra);
    }

    @Override public TaskKind getKind() { return TaskKind.WATERING; }

    @Override public String getInstructions() {
        String water = amount() == null ? null : "Give each plant about " + number(amount()) + " L of water.";
        return join(water, detail());
    }

    @Override public Optional<String> weatherHint(WeatherForecast f) {
        double rain = f.rainInNextDays(2);
        if (rain >= 10) {
            return Optional.of(String.format(Locale.ROOT,
                    "Rain is forecast (about %.0f mm in the next 2 days). You can probably skip watering.", rain));
        }
        if (f.maxTempInNextDays(1) >= 33) {
            return Optional.of("It will be hot today. Water in the early morning or late afternoon.");
        }
        return Optional.empty();
    }
}
