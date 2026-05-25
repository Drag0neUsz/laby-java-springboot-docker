package com.example.SpringBootApp.service;

import com.example.SpringBootApp.exception.*;
import com.example.SpringBootApp.model.dto.CourseDTO;
import com.example.SpringBootApp.model.dto.GradeDTO;
import com.example.SpringBootApp.model.dto.GradeDetailsDTO;
import com.example.SpringBootApp.model.dto.StudentDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GatewayService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gateway.student.url}")
    private String STUDENT_URL;

    @Value("${gateway.course.url}")
    private String COURSE_URL;

    @Value("${gateway.grade.url}")
    private String GRADE_URL;

    private void studentExists(Integer studentId) {
        try {
            restTemplate.getForObject(STUDENT_URL + studentId, StudentDTO.class);
        } catch (HttpStatusCodeException e) {
            throw new StudentNotFoundException();
        }
    }

    private void courseExists(Integer courseId) {
        try {
            restTemplate.getForObject(COURSE_URL + courseId, CourseDTO.class);
        } catch (HttpStatusCodeException e) {
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
            try {
                CourseDTO course = restTemplate.getForObject(COURSE_URL + grade.getCourseId(), CourseDTO.class);
                if (course != null) {
                    sumProduct += grade.getGrade() * course.getEcts();
                    sumEcts += course.getEcts();
                }
            } catch (Exception e) {

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
            try {
                CourseDTO course = restTemplate.getForObject(COURSE_URL + g.getCourseId(), CourseDTO.class);
                return new GradeDetailsDTO(g.getId(), g.getGrade(), course);
            } catch (Exception e) {
                return new GradeDetailsDTO(g.getId(), g.getGrade(), null);
            }
        }).toList();
    }

    @SuppressWarnings("unchecked")
    public Long countFailedStudents(Integer courseId) {
        courseExists(courseId);
        try {
            Map<String, Integer> response = restTemplate.getForObject(GRADE_URL + "course/" + courseId + "/failed/count", Map.class);
            return response != null ? response.get("failedCount").longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public List<StudentDTO> getTopStudents() {
        StudentDTO[] allStudents;

        try {
            allStudents = restTemplate.getForObject("http://localhost:8081/students", StudentDTO[].class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            throw new EmptyResultException("Cannot access student list. Service returned status: " + e.getStatusCode());
        }

        if (allStudents == null || allStudents.length == 0) {
            throw new EmptyResultException("No students saved");
        }

        List<StudentDTO> topStudents = Arrays.stream(allStudents)
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
            throw new EmptyResultException("No students with gpa >= 4.75");
        }

        return topStudents;
    }

    public List<String> getStudentCourseNames(Integer studentId) {
        studentExists(studentId);

        GradeDTO[] grades = restTemplate.getForObject(GRADE_URL + "student/" + studentId, GradeDTO[].class);
        if (grades == null || grades.length == 0) {
            throw new EmptyResultException("No courses with grades found");
        }

        return Arrays.stream(grades)
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

    /* =====================================================================
       State-changing batch operations.

       These two operations mutate the data on the student-service. They are
       implemented at the gateway level because they need to cross-reference
       Student / Grade / Course (single-service endpoints alone cannot tell
       which students must be deleted/promoted).
       ===================================================================== */

    /** Failing grade in the Polish grading scale. */
    private static final double FAIL_GRADE = 2.0;
    /** A student is purged once their failed ECTS reach this threshold. */
    private static final int FAIL_ECTS_THRESHOLD = 10;
    /** GPA threshold above which a student becomes qualified for the grant. */
    private static final double QUALIFICATION_GPA = 4.75;

    /**
     * Delete every student whose 2.0 ("failed") grades sum to at least
     * {@value #FAIL_ECTS_THRESHOLD} ECTS (each course counted once per failure).
     *
     * @return ids of the students that were actually deleted, in order.
     */
    public List<Integer> purgeStudentsWithFailedEcts() {
        StudentDTO[] allStudents;
        try {
            allStudents = restTemplate.getForObject("http://localhost:8081/students", StudentDTO[].class);
        } catch (HttpStatusCodeException e) {
            throw new EmptyResultException("Cannot access student list. Service returned status: " + e.getStatusCode());
        }
        if (allStudents == null || allStudents.length == 0) {
            return List.of();
        }

        Map<Integer, Integer> ectsCache = new HashMap<>();
        List<Integer> deleted = new ArrayList<>();

        for (StudentDTO student : allStudents) {
            int failedEcts = sumFailedEcts(student.getId(), ectsCache);
            if (failedEcts >= FAIL_ECTS_THRESHOLD) {
                try {
                    restTemplate.delete(STUDENT_URL + student.getId());
                    deleted.add(student.getId());
                } catch (Exception ignored) {
                    // Skip students that could not be deleted (already gone, etc.).
                }
            }
        }
        return deleted;
    }

    /**
     * For every student currently marked {@code NOT_QUALIFIED} whose
     * ECTS-weighted GPA is &gt;= {@value #QUALIFICATION_GPA}, promote them
     * to {@code QUALIFIED}.
     *
     * @return ids of the students that were actually promoted, in order.
     */
    public List<Integer> promoteQualifiedStudents() {
        StudentDTO[] allStudents;
        try {
            allStudents = restTemplate.getForObject("http://localhost:8081/students", StudentDTO[].class);
        } catch (HttpStatusCodeException e) {
            throw new EmptyResultException("Cannot access student list. Service returned status: " + e.getStatusCode());
        }
        if (allStudents == null || allStudents.length == 0) {
            return List.of();
        }

        List<Integer> promoted = new ArrayList<>();
        for (StudentDTO student : allStudents) {
            String current = student.getGrant();
            // Only promote students that are currently NOT_QUALIFIED.
            if (current != null && !"NOT_QUALIFIED".equalsIgnoreCase(current)) continue;

            Double gpa;
            try {
                gpa = getStudentWeightedAverage(student.getId());
            } catch (Exception ignored) {
                continue;
            }
            if (gpa == null || gpa < QUALIFICATION_GPA) continue;

            student.setGrant("QUALIFIED");
            try {
                restTemplate.exchange(
                        STUDENT_URL + student.getId(),
                        HttpMethod.PUT,
                        new HttpEntity<>(student),
                        StudentDTO.class);
                promoted.add(student.getId());
            } catch (Exception ignored) {
                // Skip students whose update fails (deleted concurrently, etc.).
            }
        }
        return promoted;
    }

    /** ECTS sum of all failed (2.0) grades for a single student. */
    private int sumFailedEcts(Integer studentId, Map<Integer, Integer> ectsCache) {
        GradeDTO[] grades;
        try {
            grades = restTemplate.getForObject(GRADE_URL + "student/" + studentId, GradeDTO[].class);
        } catch (Exception e) {
            return 0;
        }
        if (grades == null || grades.length == 0) return 0;

        int total = 0;
        for (GradeDTO g : grades) {
            if (g.getGrade() == null || g.getGrade() != FAIL_GRADE) continue;
            Integer courseId = g.getCourseId();
            if (courseId == null) continue;

            Integer ects = ectsCache.get(courseId);
            if (ects == null) {
                try {
                    CourseDTO course = restTemplate.getForObject(COURSE_URL + courseId, CourseDTO.class);
                    ects = (course == null || course.getEcts() == null) ? 0 : course.getEcts();
                } catch (Exception e) {
                    ects = 0;
                }
                ectsCache.put(courseId, ects);
            }
            total += ects;
        }
        return total;
    }

    public List<String> getCourseStudents(Integer courseId) {
        courseExists(courseId);
        GradeDTO[] grades = restTemplate.getForObject(GRADE_URL + "course/" + courseId, GradeDTO[].class);

        if (grades == null || grades.length == 0) {
            throw new EmptyResultException("No students with grades from this course");
        }

        return Arrays.stream(grades)
                .map(GradeDTO::getStudentId)
                .distinct()
                .map(studentId -> {
                    try {
                        StudentDTO student = restTemplate.getForObject(STUDENT_URL + studentId, StudentDTO.class);
                        return student != null ? student.getFirstName() : "Unknown student";
                    } catch (Exception e) {
                        return "Error getting student";
                    }
                })
                .toList();
    }
}