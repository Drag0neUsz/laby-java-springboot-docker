package com.example.SpringBootApp.exception;
public class StudentNoGradesException extends RuntimeException {
    public StudentNoGradesException() {
        super("The specified person has no grades");
    }
}