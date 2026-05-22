package com.example.SpringBootApp.exception;
public class CourseInvalidEctsException extends RuntimeException {
    public CourseInvalidEctsException() {
        super("Invalid ECTS value");
    }
}