package com.example.SpringBootApp.repository;

import com.example.SpringBootApp.model.Grade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {
    List<Grade> findByStudentId(Integer studentId);
    Long countByCourseIdAndGrade(Integer courseId, Double grade);
    List<Grade> findByCourseId(Integer courseId);
    boolean existsByCourseId(Integer courseId);
    @Transactional
    @Modifying
    void deleteByStudentId(Integer studentId);
}