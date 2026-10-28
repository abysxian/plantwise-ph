package com.agristep.service;

/** The garden's layout: how many rows and columns of beds. */
public record GridShape(int rows, int columns) {
    public int beds() { return rows * columns; }
}
