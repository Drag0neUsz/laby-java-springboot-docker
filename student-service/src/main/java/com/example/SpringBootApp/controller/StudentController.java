package com.example.SpringBootApp.controller;

import com.example.SpringBootApp.exception.StudentInvalidAgeException;
import com.example.SpringBootApp.exception.InvalidNameException;
import com.example.SpringBootApp.model.Student;
import com.example.SpringBootApp.service.StudentService;
import com.example.SpringBootApp.exception.StudentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Student> getAll() {
        return service.getAllStudents();
    }

    @GetMapping("/{id}")
    public Student get(@PathVariable int id) throws StudentNotFoundException {
        return service.getStudent(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Status 201 przy udanym dodaniu studenta
    public Student add(@RequestBody Student student) throws StudentInvalidAgeException, InvalidNameException {
        return service.addStudent(student);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable int id) throws StudentNotFoundException {
        service.deleteStudent(id);
        return ResponseEntity.ok(Collections.singletonMap("deleted", true));
    }

    @GetMapping("/count")
    public Map<String, Integer> count() {
        int studentCount = service.countStudents();
        return Collections.singletonMap("count", studentCount);
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable int id, @RequestBody Student student) throws StudentNotFoundException {
        return service.updateStudent(id, student);
    }
}