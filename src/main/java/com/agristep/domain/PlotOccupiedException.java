package com.agristep.domain;

/** Thrown when someone tries to plant in a bed that already has a crop. */
public class PlotOccupiedException extends RuntimeException {
    public PlotOccupiedException(String plotName) {
        super(plotName + " already has a crop. Harvest or clear it first.");
    }
}
