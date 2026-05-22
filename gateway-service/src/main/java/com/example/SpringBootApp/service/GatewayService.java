package com.example.SpringBootApp.service;

import com.example.SpringBootApp.exception.*;
import com.example.SpringBootApp.model.dto.CourseDTO;
import com.example.SpringBootApp.model.dto.GradeDTO;
import com.example.SpringBootApp.model.dto.GradeDetailsDTO;
import com.example.SpringBootApp.model.dto.StudentDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;

@Service
public class GatewayService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String STUDENT_URL = "http://localhost:8081/students/";
    private final String COURSE_URL = "http://localhost:8082/courses/";
    private final String GRADE_URL = "http://localhost:8083/grades/";

    private void studentExists(Integer studentId) {
        try {
            restTemplate.getForObject("http://localhost:8081/students/" + studentId, StudentDTO.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            throw new StudentNotFoundException();
        }
    }


    private void courseExists(Integer courseId) {
        try {
            restTemplate.getForObject(COURSE_URL + courseId, CourseDTO.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            throw new CourseNotFoundException();
        }
    }

    public Double getStudentWeightedAverage(Integer studentId) {
        studentExists(studentId);

        GradeDTO[] grades = restTemplate.getForObject(GRADE_URL + "student/" + studentId, GradeDTO[].class);
        if (grades == null || grades.length == 0) {
            throw new EmptyResultException("Student with ID " + studentId + " has no grades");
        }

        double sumProduct = 0.0;
        int sumEcts = 0;

        for (GradeDTO grade : grades) {
            CourseDTO course = restTemplate.getForObject(COURSE_URL + grade.getCourseId(), CourseDTO.class);
            if (course != null) {
                sumProduct += grade.getGrade() * course.getEcts();
                sumEcts += course.getEcts();
            }
        }

        return sumEcts == 0 ? 0.0 : sumProduct / sumEcts;
    }



    public List<GradeDetailsDTO> getStudentGrades(Integer studentId) {
        studentExists(studentId);

        GradeDTO[] grades = restTemplate.getForObject(GRADE_URL + "student/" + studentId, GradeDTO[].class);

        if (grades == null || grades.length == 0) {
            throw new EmptyResultException("Student with ID " + studentId + " has no grades");
        }

        return Arrays.stream(grades).map(g -> {
            CourseDTO course = restTemplate.getForObject(COURSE_URL + g.getCourseId(), CourseDTO.class);
            return new GradeDetailsDTO(g.getId(), g.getGrade(), course);
        }).toList();
    }

    public Long countFailedStudents(Integer courseId) {
        courseExists(courseId);

        return restTemplate.getForObject(GRADE_URL + "course/" + courseId + "/failed/count", Long.class);
    }

    public List<StudentDTO> getTopStudents() {
        StudentDTO[] allStudents;

        try {
            allStudents = restTemplate.getForObject("http://localhost:8081/students", StudentDTO[].class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            throw new EmptyResultException("Cannot access student list");
        }

        if (allStudents == null || allStudents.length == 0) {
            throw new EmptyResultException("No students saved");
        }

        List<StudentDTO> topStudents = java.util.Arrays.stream(allStudents)
                .filter(student -> {
                    try {
                        Double avg = getStudentWeightedAverage(student.getId());
                        return avg != null && avg >= 4.75;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
        if (topStudents.isEmpty()) {
            throw new EmptyResultException("No students with gpa > 4.75");
        }

        return topStudents;
    }

    public List<String> getStudentCourseNames(Integer studentId) {
        studentExists(studentId);

        GradeDTO[] grades = restTemplate.getForObject(GRADE_URL + "student/" + studentId, GradeDTO[].class);
        if (grades == null || grades.length == 0) {
            throw new EmptyResultException("No courses with grades found");
        }

        return java.util.Arrays.stream(grades)
                .map(GradeDTO::getCourseId)
                .distinct()
                .map(courseId -> {
                    try {
                        CourseDTO course = restTemplate.getForObject(COURSE_URL + courseId, CourseDTO.class);
                        return course != null ? course.getName() : "Unknown course";
                    } catch (Exception e) {
                        return "Error getting course name";
                    }
                })
                .toList();
    }

    public List<String> getCourseStudents(Integer courseId) {
        courseExists(courseId);
        GradeDTO[] grades = restTemplate.getForObject(GRADE_URL + "course/" + courseId, GradeDTO[].class);

        if (grades == null || grades.length == 0) {
            throw new EmptyResultException("No students with grades from this course");
        }

        return java.util.Arrays.stream(grades)
                .map(GradeDTO::getStudentId)
                .distinct()
                .map(studentId -> {
                    try {
                        StudentDTO student = restTemplate.getForObject("http://localhost:8081/students/" + studentId, StudentDTO.class);
                        return student != null ? student.getFirstName(): "Unknown student";
                    } catch (Exception e) {
                        return "Error getting student";
                    }
                })
                .toList();
    }
}