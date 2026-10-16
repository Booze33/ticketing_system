package com.booze.ticketing_system.exception;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException() {
        super("Unable to create account");
    }
}
