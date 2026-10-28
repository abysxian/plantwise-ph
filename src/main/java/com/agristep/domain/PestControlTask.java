package com.agristep.domain;

import com.agristep.weather.WeatherForecast;

import java.time.LocalDate;
import java.util.Optional;

public class PestControlTask extends CareTask {
    public PestControlTask(int dayOffset, LocalDate plantedOn, Double amount, String warningSigns, String safeRemedy) {
        super(dayOffset, plantedOn, amount, warningSigns, safeRemedy);
    }

    @Override public TaskKind getKind() { return TaskKind.PEST_CONTROL; }

    @Override public String getInstructions() {
        return "Look for " + detail() + ". Safe remedy: " + extra();
    }

    @Override public Optional<String> weatherHint(WeatherForecast f) {
        return f.rainInNextDays(3) >= 20
                ? Optional.of("Wet weather helps pests and leaf disease spread. Check under the leaves carefully.")
                : Optional.empty();
    }
}
