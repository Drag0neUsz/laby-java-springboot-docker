package com.example.SpringBootApp.exception;
public class GradeInvalidGradeException extends RuntimeException {
    public GradeInvalidGradeException() {
        super("Invalid grade");
    }
}