package com.example.SpringBootApp.exception;
public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException() {
        super("The specified course does not exist");
    }
}