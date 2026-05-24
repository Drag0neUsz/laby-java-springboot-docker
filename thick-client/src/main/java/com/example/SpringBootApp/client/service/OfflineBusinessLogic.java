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
 * Read-only analytics and "candidate finder" helpers that run STRICTLY against
 * the in-memory LocalDataStore (no REST calls). They demonstrate that the thick
 * client can produce useful answers while the network is unavailable; the
 * resulting decisions are pushed to the queue and synchronized later.
 *
 *   1. weightedGpa(studentId)             - ECTS-weighted GPA
 *   2. topStudents(threshold)             - all students with weighted GPA >= threshold
 *   3. failedCount(courseId)              - number of failing grades (== 2.0) per course
 *   4. coursesAttendedBy(studentId)       - distinct course names a student has grades for
 *   5. studentsToPurgeForFailedEcts()     - candidates for the "10 failed ECTS" purge
 *   6. studentsToPromoteToQualified()     - NOT_QUALIFIED students with GPA >= 4.75
 */
@Service
public class OfflineBusinessLogic {

    /** Failing grade in the Polish grading scale. */
    public static final double FAIL_GRADE = 2.0;
    /** A student becomes a purge candidate once their failed ECTS reach this threshold. */
    public static final int FAIL_ECTS_THRESHOLD = 10;
    /** GPA threshold above which a NOT_QUALIFIED student becomes QUALIFIED. */
    public static final double QUALIFICATION_GPA = 4.75;

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

    /**
     * Sum of ECTS of all failed (== 2.0) grades for a given student, using
     * local data only. Grades that reference an unknown / unloaded course are
     * skipped (they contribute 0 ECTS).
     */
    public int failedEctsFor(Integer studentId) {
        int total = 0;
        for (GradeDTO g : store.allGrades()) {
            if (!studentId.equals(g.getStudentId())) continue;
            if (g.getGrade() == null || g.getGrade() != FAIL_GRADE) continue;
            CourseDTO c = store.findCourse(g.getCourseId()).orElse(null);
            if (c == null || c.getEcts() == null) continue;
            total += c.getEcts();
        }
        return total;
    }

    /**
     * Students whose failed-ECTS total is at least {@value #FAIL_ECTS_THRESHOLD};
     * each entry is (student, failedEcts).
     */
    public List<Map.Entry<StudentDTO, Integer>> studentsToPurgeForFailedEcts() {
        List<Map.Entry<StudentDTO, Integer>> result = new ArrayList<>();
        for (StudentDTO s : store.allStudents()) {
            int failed = failedEctsFor(s.getId());
            if (failed >= FAIL_ECTS_THRESHOLD) {
                result.add(Map.entry(s, failed));
            }
        }
        result.sort(Comparator.<Map.Entry<StudentDTO, Integer>, Integer>comparing(Map.Entry::getValue).reversed());
        return result;
    }

    /**
     * Students currently {@code NOT_QUALIFIED} (null counts as such) whose
     * weighted GPA is at least {@value #QUALIFICATION_GPA}.
     * Each entry is (student, gpa).
     */
    public List<Map.Entry<StudentDTO, Double>> studentsToPromoteToQualified() {
        List<Map.Entry<StudentDTO, Double>> result = new ArrayList<>();
        for (StudentDTO s : store.allStudents()) {
            String grant = s.getGrant();
            boolean notQualified = grant == null
                    || grant.isBlank()
                    || StudentDTO.GRANT_NOT_QUALIFIED.equalsIgnoreCase(grant);
            if (!notQualified) continue;

            Double gpa = weightedGpa(s.getId());
            if (gpa != null && gpa >= QUALIFICATION_GPA) {
                result.add(Map.entry(s, gpa));
            }
        }
        result.sort(Comparator.<Map.Entry<StudentDTO, Double>, Double>comparing(Map.Entry::getValue).reversed());
        return result;
    }
}
