package com.example.SpringBootApp.service;

import com.example.SpringBootApp.exception.CourseNotFoundException;
import com.example.SpringBootApp.exception.GradeInvalidGradeException;
import com.example.SpringBootApp.exception.GradeNotFoundException;

import com.example.SpringBootApp.exception.StudentNotFoundException;
import com.example.SpringBootApp.model.Grade;

import com.example.SpringBootApp.repository.GradeRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class GradeServiceImpl implements GradeService {
    private final List<Double> allowedGrades = Arrays.asList(2.0, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5);
    private void validateGradeValue(Double value) {
        if (value == null || !allowedGrades.contains(value)) {
            throw new GradeInvalidGradeException();
        }
    }

    private final GradeRepository gradeRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public GradeServiceImpl(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    @Override
    public List<Grade> getAllGrades() {
        return gradeRepository.findAll();
    }

    @Override
    public Grade getGrade(Integer id) throws GradeNotFoundException {
        return gradeRepository.findById(id)
                .orElseThrow(GradeNotFoundException::new);
    }

    @Override
    public Grade addGrade(Grade grade) throws GradeInvalidGradeException {
        validateGradeValue(grade.getGrade());

        try {
            restTemplate.getForObject("http://localhost:8081/students/" + grade.getStudentId(), Object.class);
        } catch (HttpStatusCodeException e) {
            throw new StudentNotFoundException();
        }
        
        try {
            restTemplate.getForObject("http://localhost:8082/courses/" + grade.getCourseId(), Object.class);
        } catch (HttpStatusCodeException e) {
            throw new CourseNotFoundException();
        }

        return gradeRepository.save(grade);
    }

    @Override
    public Grade updateGrade(Integer id, Grade gradeDetails) throws GradeNotFoundException, GradeInvalidGradeException {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(GradeNotFoundException::new);

        validateGradeValue(gradeDetails.getGrade());

        grade.setGrade(gradeDetails.getGrade());
        return gradeRepository.save(grade);
    }

    @Override
    public boolean deleteGrade(Integer id) throws GradeNotFoundException {
        if (!gradeRepository.existsById(id)) {
            throw new GradeNotFoundException();
        }
        gradeRepository.deleteById(id);
        return true;
    }



    @Override
    public List<Grade> findByStudentId(Integer studentId) {
        return gradeRepository.findByStudentId(studentId);
    }

    @Override
    public List<Grade> findByCourseId(Integer courseId) {
        return gradeRepository.findByCourseId(courseId);
    }

    @Override
    public Long countByCourseIdAndGrade(Integer courseId, Double grade) {
        return gradeRepository.countByCourseIdAndGrade(courseId, grade);
    }

    @Override
    public boolean existsByCourseId(Integer courseId) {
        return gradeRepository.existsByCourseId(courseId);
    }

    public void deleteByStudentId(Integer studentId) {gradeRepository.deleteByStudentId(studentId);}
}