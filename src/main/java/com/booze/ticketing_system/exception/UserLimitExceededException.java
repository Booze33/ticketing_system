package com.booze.ticketing_system.exception;

public class UserLimitExceededException extends RuntimeException {
    public UserLimitExceededException(String message) {
        super(message);
    }
}