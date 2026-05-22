package com.example.SpringBootApp.service;

import com.example.SpringBootApp.exception.*;
import com.example.SpringBootApp.model.Course;
//import com.example.SpringBootApp.model.Grade;

import java.util.List;

public interface CourseService {
    List<Course> getAllCourses();
    Course getCourseById(Integer id);
    Course addCourse(Course course) throws InvalidNameException, CourseInvalidEctsException;
    Course updateCourse(Integer id, Course courseDetails) throws InvalidNameException, CourseInvalidEctsException;
    boolean deleteCourse(Integer id) throws CourseNotFoundException, CourseHasGradesException;
    List<Course> getCoursesByEcts(Integer ects) throws CourseNotFoundException;
}