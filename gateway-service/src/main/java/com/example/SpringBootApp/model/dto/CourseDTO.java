package com.example.SpringBootApp.model.dto;

public class CourseDTO {
    private Integer id;
    private String name;
    private Integer ects;

    public CourseDTO(){}

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Integer getEcts() {
        return ects;
    }
    public void setEcts(Integer ects) {
        this.ects = ects;
    }
}
