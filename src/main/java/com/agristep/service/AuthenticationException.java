package com.agristep.service;

/** Sign-in failed (wrong details, or too many tries). The message is safe to show on screen. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) { super(message); }
}
