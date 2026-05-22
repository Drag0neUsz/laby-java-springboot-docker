package com.example.SpringBootApp.exception;
public class GradeNotFoundException extends RuntimeException {
    public GradeNotFoundException() {
        super("The specified grade does not exist");
    }
}