package com.agristep.service;

/** A garden operation was attempted while nobody is signed in. This is a programming error, not a user mistake. */
public class NotSignedInException extends IllegalStateException {
    public NotSignedInException() { super("Nobody is signed in."); }
}
