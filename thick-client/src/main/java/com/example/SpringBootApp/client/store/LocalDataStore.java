package com.example.SpringBootApp.client.store;

import com.example.SpringBootApp.client.model.dto.CourseDTO;
import com.example.SpringBootApp.client.model.dto.GradeDTO;
import com.example.SpringBootApp.client.model.dto.StudentDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory replica of the distributed system state.
 *
 * Holds three working maps (one per entity) plus a parallel "original snapshot"
 * map per entity that captures the server state at the moment of the last fetch.
 * The snapshots are used by the sync engine to detect modification conflicts
 * (i.e. cases where the server-side value drifted from what we last saw).
 *
 * Entities created locally while offline get temporary NEGATIVE ids so they
 * cannot collide with real server-assigned ids.
 */
@Component
public class LocalDataStore {

    private final Map<Integer, StudentDTO> students = new HashMap<>();
    private final Map<Integer, CourseDTO> courses = new HashMap<>();
    private final Map<Integer, GradeDTO> grades = new HashMap<>();

    private final Map<Integer, StudentDTO> studentSnapshots = new HashMap<>();
    private final Map<Integer, CourseDTO> courseSnapshots = new HashMap<>();
    private final Map<Integer, GradeDTO> gradeSnapshots = new HashMap<>();

    private final AtomicInteger tempIdSeq = new AtomicInteger(0);

    public int nextTempId() {
        return tempIdSeq.decrementAndGet();
    }

    public void clear() {
        students.clear();
        courses.clear();
        grades.clear();
        studentSnapshots.clear();
        courseSnapshots.clear();
        gradeSnapshots.clear();
    }

    public void replaceStudents(Collection<StudentDTO> incoming) {
        students.clear();
        studentSnapshots.clear();
        for (StudentDTO s : incoming) {
            students.put(s.getId(), s);
            studentSnapshots.put(s.getId(), s.copy());
        }
    }

    public void replaceCourses(Collection<CourseDTO> incoming) {
        courses.clear();
        courseSnapshots.clear();
        for (CourseDTO c : incoming) {
            courses.put(c.getId(), c);
            courseSnapshots.put(c.getId(), c.copy());
        }
    }

    public void replaceGrades(Collection<GradeDTO> incoming) {
        grades.clear();
        gradeSnapshots.clear();
        for (GradeDTO g : incoming) {
            grades.put(g.getId(), g);
            gradeSnapshots.put(g.getId(), g.copy());
        }
    }

    public void putStudent(StudentDTO s) { students.put(s.getId(), s); }
    public void putCourse(CourseDTO c) { courses.put(c.getId(), c); }
    public void putGrade(GradeDTO g) { grades.put(g.getId(), g); }

    public void removeStudent(Integer id) { students.remove(id); }
    public void removeCourse(Integer id) { courses.remove(id); }
    public void removeGrade(Integer id) { grades.remove(id); }

    public Optional<StudentDTO> findStudent(Integer id) { return Optional.ofNullable(students.get(id)); }
    public Optional<CourseDTO> findCourse(Integer id) { return Optional.ofNullable(courses.get(id)); }
    public Optional<GradeDTO> findGrade(Integer id) { return Optional.ofNullable(grades.get(id)); }

    public List<StudentDTO> allStudents() { return new ArrayList<>(students.values()); }
    public List<CourseDTO> allCourses() { return new ArrayList<>(courses.values()); }
    public List<GradeDTO> allGrades() { return new ArrayList<>(grades.values()); }

    public Optional<StudentDTO> studentSnapshot(Integer id) { return Optional.ofNullable(studentSnapshots.get(id)); }
    public Optional<CourseDTO> courseSnapshot(Integer id) { return Optional.ofNullable(courseSnapshots.get(id)); }
    public Optional<GradeDTO> gradeSnapshot(Integer id) { return Optional.ofNullable(gradeSnapshots.get(id)); }

    /**
     * After a successful POST/PUT against the server, refresh the snapshot so
     * a follow-up sync of the same entity does not falsely flag a conflict.
     */
    public void refreshStudentSnapshot(StudentDTO authoritative) {
        studentSnapshots.put(authoritative.getId(), authoritative.copy());
    }
    public void refreshCourseSnapshot(CourseDTO authoritative) {
        courseSnapshots.put(authoritative.getId(), authoritative.copy());
    }
    public void refreshGradeSnapshot(GradeDTO authoritative) {
        gradeSnapshots.put(authoritative.getId(), authoritative.copy());
    }

    /**
     * Reassign id from temporary (negative) to server-assigned id after a CREATE
     * is successfully synchronized.
     */
    public void promoteStudentId(Integer tempId, StudentDTO serverEntity) {
        students.remove(tempId);
        students.put(serverEntity.getId(), serverEntity);
        studentSnapshots.remove(tempId);
        studentSnapshots.put(serverEntity.getId(), serverEntity.copy());
    }
    public void promoteCourseId(Integer tempId, CourseDTO serverEntity) {
        courses.remove(tempId);
        courses.put(serverEntity.getId(), serverEntity);
        courseSnapshots.remove(tempId);
        courseSnapshots.put(serverEntity.getId(), serverEntity.copy());
    }
    public void promoteGradeId(Integer tempId, GradeDTO serverEntity) {
        grades.remove(tempId);
        grades.put(serverEntity.getId(), serverEntity);
        gradeSnapshots.remove(tempId);
        gradeSnapshots.put(serverEntity.getId(), serverEntity.copy());
    }

    /**
     * After a successful DELETE we drop the snapshot too so the entity disappears
     * from "what we last saw on the server".
     */
    public void dropStudentSnapshot(Integer id) { studentSnapshots.remove(id); }
    public void dropCourseSnapshot(Integer id) { courseSnapshots.remove(id); }
    public void dropGradeSnapshot(Integer id) { gradeSnapshots.remove(id); }
}
