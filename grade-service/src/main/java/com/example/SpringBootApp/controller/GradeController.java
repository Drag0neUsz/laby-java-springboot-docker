package com.example.SpringBootApp.controller;

import com.example.SpringBootApp.exception.GradeNotFoundException;
import com.example.SpringBootApp.model.Grade;
import com.example.SpringBootApp.service.GradeService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping
    public List<Grade> getAll() {
        return gradeService.getAllGrades();
    }

    @GetMapping("/{id}")
    public Grade get(@PathVariable Integer id) throws GradeNotFoundException {
        return gradeService.getGrade(id);
    }

    @PostMapping
    public Grade add(@RequestBody Grade grade) {
        return gradeService.addGrade(grade);
    }

    @PutMapping("/{id}")
    public Grade update(@PathVariable Integer id, @RequestBody Grade grade) throws GradeNotFoundException {
        return gradeService.updateGrade(id, grade);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return gradeService.deleteGrade(id);
    }



    @GetMapping("/student/{studentId}")
    public List<Grade> getByStudentId(@PathVariable Integer studentId) {
        return gradeService.findByStudentId(studentId);
    }

    @GetMapping("/course/{courseId}")
    public List<Grade> getByCourseId(@PathVariable Integer courseId) {
        return gradeService.findByCourseId(courseId);
    }

    @GetMapping("/course/{courseId}/failed/count")
    public Long getFailedCount(@PathVariable Integer courseId) {
        return gradeService.countByCourseIdAndGrade(courseId, 2.0);
    }

    @GetMapping("/course/{courseId}/exists")
    public ResponseEntity<Boolean> existsByCourseId(@PathVariable Integer courseId) {
        boolean exists = gradeService.existsByCourseId(courseId);
        return ResponseEntity.ok(exists);
    }

    @DeleteMapping("/student/{studentId}")
    public void deleteGradesByStudentId(@PathVariable Integer studentId) {
        gradeService.deleteByStudentId(studentId);
    }
}