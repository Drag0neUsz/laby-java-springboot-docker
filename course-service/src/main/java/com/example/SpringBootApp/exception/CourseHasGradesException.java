package com.example.SpringBootApp.exception;
public class CourseHasGradesException extends RuntimeException {
    public CourseHasGradesException() {
        super("Course has existing grades");
    }
}