package com.example.SpringBootApp.service;

import com.example.SpringBootApp.exception.GradeInvalidGradeException;
import com.example.SpringBootApp.exception.GradeNotFoundException;

import com.example.SpringBootApp.model.Grade;


import java.util.List;

public interface GradeService {
    List<Grade> getAllGrades();
    Grade getGrade(Integer id) throws GradeNotFoundException;
    Grade addGrade(Grade grade) throws GradeInvalidGradeException;
    Grade updateGrade(Integer id, Grade gradeDetails) throws GradeNotFoundException, GradeInvalidGradeException;
    boolean deleteGrade(Integer id) throws GradeNotFoundException;

    void deleteByStudentId(Integer studentId);
    List<Grade> findByStudentId(Integer studentId);
    List<Grade> findByCourseId(Integer courseId);
    Long countByCourseIdAndGrade(Integer courseId, Double grade);
    boolean existsByCourseId(Integer courseId);
}
