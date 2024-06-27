package com.amazonaws.autoloop.mockserver.e2etests;

public class WrongResultException extends RuntimeException {
    public WrongResultException(String message) {
        super(message);
    }
}
