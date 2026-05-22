package com.example.SpringBootApp.service;

import com.example.SpringBootApp.exception.*;

import com.example.SpringBootApp.model.Student;
import com.example.SpringBootApp.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Student getStudent(Integer id) throws StudentNotFoundException {
        return studentRepository.findById(id)
                .orElseThrow(StudentNotFoundException::new);
    }

    @Override
    public Student addStudent(Student student) throws StudentInvalidAgeException, InvalidNameException {
        if (student.getAge() <= 0 || student.getAge() > 123) throw new StudentInvalidAgeException();
        if (student.getFirstName() == null || student.getFirstName().isEmpty()) throw new InvalidNameException();
        return studentRepository.save(student);
    }

    private final RestTemplate restTemplate = new RestTemplate();
    @Override
    public boolean deleteStudent(Integer id) {
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException();
        }

        try {
            restTemplate.delete("http://localhost:8083/grades/student/" + id);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        studentRepository.deleteById(id);
        return true;
    }

    @Override
    public Student updateStudent(Integer id, Student studentDetails) throws StudentNotFoundException {
        Student student = studentRepository.findById(id)
                .orElseThrow(StudentNotFoundException::new);
        student.setFirstName(studentDetails.getFirstName());
        student.setAge(studentDetails.getAge());
        student.setCity(studentDetails.getCity());
        return studentRepository.save(student);
    }


    @Override
    public int countStudents() {
        return (int) studentRepository.count();
    }



}