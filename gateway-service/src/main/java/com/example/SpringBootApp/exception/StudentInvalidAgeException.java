package com.example.SpringBootApp.exception;
public class StudentInvalidAgeException extends RuntimeException {
    public StudentInvalidAgeException() {
        super("Invalid Age");
    }
}