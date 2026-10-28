package com.agristep.domain;

/** Thrown when a requested bed, crop or task does not exist. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
