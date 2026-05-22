package com.example.SpringBootApp.controller;

import com.example.SpringBootApp.exception.GradeInvalidGradeException;
import com.example.SpringBootApp.exception.GradeNotFoundException;
import com.example.SpringBootApp.model.Grade;
import com.example.SpringBootApp.service.GradeService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    @ResponseStatus(HttpStatus.CREATED)
    public Grade add(@RequestBody Grade grade) throws GradeInvalidGradeException {
        return gradeService.addGrade(grade);
    }

    @PutMapping("/{id}")
    public Grade update(@PathVariable Integer id, @RequestBody Grade grade)
            throws GradeNotFoundException, GradeInvalidGradeException { // Dodana poprawna deklaracja wyjątków
        return gradeService.updateGrade(id, grade);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable Integer id) throws GradeNotFoundException {
        gradeService.deleteGrade(id);
        return ResponseEntity.ok(Collections.singletonMap("deleted", true));
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
    public Map<String, Long> getFailedCount(@PathVariable Integer courseId) {
        long count = gradeService.countByCourseIdAndGrade(courseId, 2.0);
        return Collections.singletonMap("failedCount", count);
    }

    @GetMapping("/course/{courseId}/exists")
    public Map<String, Boolean> existsByCourseId(@PathVariable Integer courseId) {
        boolean exists = gradeService.existsByCourseId(courseId);
        return Collections.singletonMap("exists", exists);
    }

    @DeleteMapping("/student/{studentId}")
    public ResponseEntity<Map<String, Boolean>> deleteGradesByStudentId(@PathVariable Integer studentId) {
        gradeService.deleteByStudentId(studentId);
        return ResponseEntity.ok(Collections.singletonMap("deletedAllStudentGrades", true));
    }
}