package com.example.exception;

public class WarmestNotFoundException extends RuntimeException {
    public WarmestNotFoundException() {
        super("Warmest not found");
    }
}
