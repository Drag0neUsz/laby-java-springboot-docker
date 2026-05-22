package com.example.SpringBootApp.model.dto;

import com.example.SpringBootApp.model.dto.CourseDTO;

public class GradeDetailsDTO {

    private Integer id;
    private Double grade;
    private CourseDTO course;

    public GradeDetailsDTO() {
    }

    public GradeDetailsDTO(Integer id, Double grade, CourseDTO course) {
        this.id = id;
        this.grade = grade;
        this.course = course;
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public Double getGrade() {
        return grade;
    }
    public void setGrade(Double grade) {
        this.grade = grade;
    }

    public CourseDTO getCourse() {
        return course;
    }
    public void setCourse(CourseDTO course) {
        this.course = course;
    }
}