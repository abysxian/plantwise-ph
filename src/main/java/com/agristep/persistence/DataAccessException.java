package com.agristep.persistence;

/** A database operation failed. Wraps the checked SQLException so the rest of the app stays simple. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
