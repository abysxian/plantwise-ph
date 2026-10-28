package com.agristep.domain;

import com.agristep.weather.WeatherForecast;

import java.time.LocalDate;
import java.util.Optional;

public class FeedingTask extends CareTask {
    public FeedingTask(int dayOffset, LocalDate plantedOn, Double amount, String detail, String extra) {
        super(dayOffset, plantedOn, amount, detail, extra);
    }

    @Override public TaskKind getKind() { return TaskKind.FEEDING; }

    @Override public String getInstructions() { return join(detail(), extra()); }

    @Override public Optional<String> weatherHint(WeatherForecast f) {
        return f.rainInNextDays(2) >= 25
                ? Optional.of("Heavy rain is coming and can wash fertiliser away. Feed after it stops.")
                : Optional.empty();
    }
}
