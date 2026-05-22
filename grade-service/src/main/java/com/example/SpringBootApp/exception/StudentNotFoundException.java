package com.example.SpringBootApp.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException() {
        super("The specified person does not exist");
    }
}