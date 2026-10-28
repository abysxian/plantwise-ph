package com.agristep.domain;

import com.agristep.weather.WeatherForecast;

import java.time.LocalDate;
import java.util.Optional;

public class SowingTask extends CareTask {
    public SowingTask(int dayOffset, LocalDate plantedOn, Double amount, String detail, String extra) {
        super(dayOffset, plantedOn, amount, detail, extra);
    }

    @Override public TaskKind getKind() { return TaskKind.SOWING; }

    @Override public String getInstructions() {
        String depth = amount() == null ? null : "Sow the seeds about " + number(amount()) + " cm deep.";
        return join(depth, detail());
    }

    @Override public Optional<String> weatherHint(WeatherForecast f) {
        return f.rainInNextDays(2) >= 25
                ? Optional.of("Heavy rain can wash seeds out. Cover the bed or sow after the rain.")
                : Optional.empty();
    }
}
