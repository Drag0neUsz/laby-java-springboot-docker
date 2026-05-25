package com.example.SpringBootApp.model.dto;

import java.util.Objects;

public class CourseDTO {
    private Integer id;
    private String name;
    private Integer ects;

    public CourseDTO() {}

    public CourseDTO(Integer id, String name, Integer ects) {
        this.id = id;
        this.name = name;
        this.ects = ects;
    }

    public CourseDTO copy() {
        return new CourseDTO(id, name, ects);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getEcts() { return ects; }
    public void setEcts(Integer ects) { this.ects = ects; }

    public boolean fieldsEqual(CourseDTO other) {
        if (other == null) return false;
        return Objects.equals(name, other.name)
                && Objects.equals(ects, other.ects);
    }

    @Override
    public String toString() {
        return "Course{id=" + id + ", name='" + name + "', ects=" + ects + "}";
    }
}
