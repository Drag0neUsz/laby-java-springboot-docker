package com.example.SpringBootApp.model.dto;

import java.util.Objects;

public class GradeDTO {
    private Integer id;
    private Double grade;
    private Integer studentId;
    private Integer courseId;

    public GradeDTO() {}

    public GradeDTO(Integer id, Double grade, Integer studentId, Integer courseId) {
        this.id = id;
        this.grade = grade;
        this.studentId = studentId;
        this.courseId = courseId;
    }

    public GradeDTO copy() {
        return new GradeDTO(id, grade, studentId, courseId);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public boolean fieldsEqual(GradeDTO other) {
        if (other == null) return false;
        return Objects.equals(grade, other.grade)
                && Objects.equals(studentId, other.studentId)
                && Objects.equals(courseId, other.courseId);
    }

    @Override
    public String toString() {
        return "Grade{id=" + id + ", grade=" + grade + ", studentId=" + studentId + ", courseId=" + courseId + "}";
    }
}
