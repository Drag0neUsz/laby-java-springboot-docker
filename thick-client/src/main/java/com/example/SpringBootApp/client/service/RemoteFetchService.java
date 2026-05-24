package com.example.SpringBootApp.client.service;

import com.example.SpringBootApp.client.model.dto.CourseDTO;
import com.example.SpringBootApp.client.model.dto.GradeDTO;
import com.example.SpringBootApp.client.model.dto.StudentDTO;
import com.example.SpringBootApp.client.store.LocalDataStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * READ side of the thick client - issues GET requests against the three
 * microservices and refreshes the LocalDataStore. Write/sync logic lives in
 * SyncService instead.
 */
@Service
public class RemoteFetchService {

    private final RestTemplate restTemplate;
    private final LocalDataStore store;

    private final String studentUrl;
    private final String courseUrl;
    private final String gradeUrl;

    public RemoteFetchService(RestTemplate restTemplate,
                              LocalDataStore store,
                              @Value("${thick-client.student-service-url:http://localhost:8081}") String studentBase,
                              @Value("${thick-client.course-service-url:http://localhost:8082}") String courseBase,
                              @Value("${thick-client.grade-service-url:http://localhost:8083}") String gradeBase) {
        this.restTemplate = restTemplate;
        this.store = store;
        this.studentUrl = trim(studentBase) + "/students";
        this.courseUrl = trim(courseBase) + "/courses";
        this.gradeUrl = trim(gradeBase) + "/grades";
    }

    private static String trim(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    public String studentUrl() { return studentUrl; }
    public String courseUrl() { return courseUrl; }
    public String gradeUrl() { return gradeUrl; }

    public void fetchAll() {
        System.out.println("[FETCH] GET " + studentUrl);
        List<StudentDTO> students = safeFetchList(studentUrl, StudentDTO[].class);

        System.out.println("[FETCH] GET " + courseUrl);
        List<CourseDTO> courses = safeFetchList(courseUrl, CourseDTO[].class);

        System.out.println("[FETCH] GET " + gradeUrl);
        List<GradeDTO> grades = safeFetchList(gradeUrl, GradeDTO[].class);

        store.replaceStudents(students);
        store.replaceCourses(courses);
        store.replaceGrades(grades);

        System.out.println("[FETCH] Local store refreshed: "
                + students.size() + " student(s), "
                + courses.size() + " course(s), "
                + grades.size() + " grade(s).");
    }

    private <T> List<T> safeFetchList(String url, Class<T[]> arrayType) {
        try {
            T[] arr = restTemplate.getForObject(url, arrayType);
            return arr == null ? Collections.emptyList() : Arrays.asList(arr);
        } catch (Exception e) {
            System.out.println("[FETCH][WARN] Failed to fetch " + url + " -> " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch the latest server state of a single Student, or null if it was deleted.
     */
    public StudentDTO fetchStudent(Integer id) {
        try {
            return restTemplate.getForObject(studentUrl + "/" + id, StudentDTO.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw e;
        }
    }

    public CourseDTO fetchCourse(Integer id) {
        try {
            return restTemplate.getForObject(courseUrl + "/" + id, CourseDTO.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw e;
        }
    }

    public GradeDTO fetchGrade(Integer id) {
        try {
            return restTemplate.getForObject(gradeUrl + "/" + id, GradeDTO.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw e;
        }
    }
}
