package com.example.SpringBootApp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GradeNotFoundException extends RuntimeException {
    public GradeNotFoundException() {
        super("The specified grade does not exist");
    }
}