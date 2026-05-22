package com.example.SpringBootApp.controller;

import com.example.SpringBootApp.model.dto.GradeDetailsDTO;
import com.example.SpringBootApp.model.dto.StudentDTO;
import com.example.SpringBootApp.service.GatewayService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @GetMapping("/students/{id}/gpa")
    public Map<String, Double> getAverage(@PathVariable Integer id) {
        Double gpa = gatewayService.getStudentWeightedAverage(id);
        return Collections.singletonMap("gpa", gpa);
    }

    @GetMapping("/students/{id}/grades")
    public List<GradeDetailsDTO> getGrades(@PathVariable Integer id) {
        return gatewayService.getStudentGrades(id);
    }

    @GetMapping("/courses/{id}/failed")
    public Map<String, Long> getFailedCount(@PathVariable Integer id) {
        Long count = gatewayService.countFailedStudents(id);
        return Collections.singletonMap("failedCount", count);
    }

    @GetMapping("/students/top")
    public List<StudentDTO> getTopStudents() {
        return gatewayService.getTopStudents();
    }

    @GetMapping("/students/{id}/graded-courses")
    public Map<String, List<String>> getStudentCourseNames(@PathVariable Integer id) {
        List<String> courseNames = gatewayService.getStudentCourseNames(id);
        return Collections.singletonMap("courses", courseNames);
    }

    @GetMapping("/courses/{id}/graded-students")
    public Map<String, List<String>> getCourseStudents(@PathVariable Integer id) {
        List<String> studentNames = gatewayService.getCourseStudents(id);
        return Collections.singletonMap("students", studentNames);
    }
}