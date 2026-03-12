package com.example.exception;

public class KeyNotFoundException extends RuntimeException{

    public KeyNotFoundException(String key){
        super(String.format("Key not found: %s", key));
    }

}
