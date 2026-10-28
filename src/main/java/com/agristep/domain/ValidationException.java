package com.agristep.domain;

/** The person typed something that can't be accepted. The message is written to be shown on screen as it is. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
