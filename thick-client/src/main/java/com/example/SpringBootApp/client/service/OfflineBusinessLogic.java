package com.example.SpringBootApp.client.service;

import com.example.SpringBootApp.client.model.dto.CourseDTO;
import com.example.SpringBootApp.client.model.dto.GradeDTO;
import com.example.SpringBootApp.client.model.dto.StudentDTO;
import com.example.SpringBootApp.client.store.LocalDataStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A handful of read-only analytics that run STRICTLY against the in-memory
 * LocalDataStore (no REST calls). They demonstrate that the thick client can
 * produce useful answers while the network is unavailable.
 *
 *   1. weightedGpa(studentId)            - ECTS-weighted GPA
 *   2. topStudents(threshold)            - all students with weighted GPA >= threshold
 *   3. failedCount(courseId)             - number of failing grades (== 2.0) per course
 *   4. coursesAttendedBy(studentId)      - distinct course names a student has grades for
 */
@Service
public class OfflineBusinessLogic {

    private final LocalDataStore store;

    public OfflineBusinessLogic(LocalDataStore store) {
        this.store = store;
    }

    /**
     * ECTS-weighted GPA computed from local data only.
     * Returns null if the student has no grades or no relevant courses are loaded.
     */
    public Double weightedGpa(Integer studentId) {
        List<GradeDTO> grades = store.allGrades().stream()
                .filter(g -> studentId.equals(g.getStudentId()))
                .toList();
        if (grades.isEmpty()) return null;

        double weighted = 0.0;
        int totalEcts = 0;
        for (GradeDTO g : grades) {
            CourseDTO c = store.findCourse(g.getCourseId()).orElse(null);
            if (c == null || c.getEcts() == null || g.getGrade() == null) continue;
            weighted += g.getGrade() * c.getEcts();
            totalEcts += c.getEcts();
        }
        return totalEcts == 0 ? null : weighted / totalEcts;
    }

    public List<Map.Entry<StudentDTO, Double>> topStudents(double threshold) {
        List<Map.Entry<StudentDTO, Double>> result = new ArrayList<>();
        for (StudentDTO s : store.allStudents()) {
            Double gpa = weightedGpa(s.getId());
            if (gpa != null && gpa >= threshold) {
                result.add(Map.entry(s, gpa));
            }
        }
        result.sort(Comparator.<Map.Entry<StudentDTO, Double>, Double>comparing(Map.Entry::getValue).reversed());
        return result;
    }

    public long failedCount(Integer courseId) {
        return store.allGrades().stream()
                .filter(g -> courseId.equals(g.getCourseId()))
                .filter(g -> g.getGrade() != null && g.getGrade() == 2.0)
                .count();
    }

    public List<String> coursesAttendedBy(Integer studentId) {
        return store.allGrades().stream()
                .filter(g -> studentId.equals(g.getStudentId()))
                .map(GradeDTO::getCourseId)
                .distinct()
                .map(cid -> store.findCourse(cid).map(CourseDTO::getName).orElse("Unknown course #" + cid))
                .toList();
    }
}
