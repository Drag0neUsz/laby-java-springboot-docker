package com.example.SpringBootApp.exception;
public class InvalidNameException extends RuntimeException {
    public InvalidNameException() {
        super("Invalid Name");
    }
}