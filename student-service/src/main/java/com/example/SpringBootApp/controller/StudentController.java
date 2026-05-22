package com.example.SpringBootApp.controller;

import com.example.SpringBootApp.exception.StudentInvalidAgeException;
import com.example.SpringBootApp.exception.InvalidNameException;
import com.example.SpringBootApp.model.Student;
import com.example.SpringBootApp.service.StudentService;
import com.example.SpringBootApp.exception.StudentNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Student add(@RequestBody Student student) throws StudentInvalidAgeException, InvalidNameException {
        return service.addStudent(student);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable int id) throws StudentNotFoundException {
        return service.deleteStudent(id);
    }

    @GetMapping("/count")
    public int count() {
        return service.countStudents();
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable int id, @RequestBody Student student) throws StudentNotFoundException {
        return service.updateStudent(id, student);
    }
}