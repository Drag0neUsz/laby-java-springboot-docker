package com.example.SpringBootApp.controller;

import com.example.SpringBootApp.exception.CourseNotFoundException;
//import com.example.SpringBootApp.exception.GradeNotFoundException;
import com.example.SpringBootApp.model.Course;
//import com.example.SpringBootApp.model.Grade;
import com.example.SpringBootApp.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<Course> getAll() {
        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    public Course getById(@PathVariable Integer id) {
        return courseService.getCourseById(id);
    }

    @PostMapping
    public Course add(@RequestBody Course course) {
        return courseService.addCourse(course);
    }

    @PutMapping("/{id}")
    public Course update(@PathVariable Integer id, @RequestBody Course course) {
        return courseService.updateCourse(id, course);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return courseService.deleteCourse(id);
    }

    @GetMapping("/ects/{ects}")
    public List<Course> getByEcts(@PathVariable Integer ects) {
        return courseService.getCoursesByEcts(ects);
    }

//    @GetMapping("/{id}/grades")
//    public List<Grade> getGrades(@PathVariable Integer id) throws CourseNotFoundException, GradeNotFoundException {
//        return courseService.getCourseGrades(id);
//    }


}