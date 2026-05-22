package com.example.SpringBootApp.repository;

import com.example.SpringBootApp.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Integer> {
    List<Course> findByEcts(int ects);
}
