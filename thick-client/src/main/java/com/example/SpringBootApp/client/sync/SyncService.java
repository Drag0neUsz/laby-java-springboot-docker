package com.example.SpringBootApp.client.sync;

import com.example.SpringBootApp.client.model.dto.CourseDTO;
import com.example.SpringBootApp.client.model.dto.GradeDTO;
import com.example.SpringBootApp.client.model.dto.StudentDTO;
import com.example.SpringBootApp.client.service.RemoteFetchService;
import com.example.SpringBootApp.client.store.LocalDataStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Push side of the thick client.
 *
 * Pipeline executed by {@link #synchronize(Scanner)}:
 *   1. Snapshot the current changelog and pass it through the SyncOptimizer.
 *   2. Walk the optimized queue and execute the corresponding REST calls
 *      (POST / PUT / DELETE) using a single RestTemplate.
 *   3. Detect and report all three flavours of conflict required by the spec:
 *        - Deletion conflict   (server returned 404 on UPDATE/DELETE)
 *        - Modification conflict (server-state drifted from the snapshot we
 *          fetched - asks the user Y/N before overwriting)
 *        - Business-rule conflict (the offline action references a parent
 *          entity that no longer exists on the server)
 *   4. Successful records are removed from the queue. Records that the user
 *      declined to resolve immediately stay queued AND are added to a
 *      "pending conflicts" list reviewable through menu option (f).
 *   5. Local temp ids (e.g. -1) created offline are mapped to the
 *      server-assigned positive ids; foreign-key references inside still-queued
 *      records get rewritten on the fly.
 */
@Service
public class SyncService {

    private final RestTemplate restTemplate;
    private final ActionQueue queue;
    private final SyncOptimizer optimizer;
    private final RemoteFetchService remote;
    private final LocalDataStore store;

    private final List<PendingConflict> pendingConflicts = new ArrayList<>();

    public SyncService(RestTemplate restTemplate,
                       ActionQueue queue,
                       SyncOptimizer optimizer,
                       RemoteFetchService remote,
                       LocalDataStore store) {
        this.restTemplate = restTemplate;
        this.queue = queue;
        this.optimizer = optimizer;
        this.remote = remote;
        this.store = store;
    }

    public List<PendingConflict> getPendingConflicts() {
        return new ArrayList<>(pendingConflicts);
    }

    public void clearPendingConflicts() {
        pendingConflicts.clear();
    }

    public void removePendingConflict(PendingConflict pc) {
        pendingConflicts.remove(pc);
    }

    /**
     * Run the full sync pipeline against the three microservices.
     *
     * @param userInput console scanner used to ask the user Y/N during
     *                  interactive conflict resolution
     */
    public void synchronize(Scanner userInput) {
        if (queue.isEmpty()) {
            System.out.println("[SYNC] Nothing to synchronize - the change queue is empty.");
            return;
        }

        List<ChangeRecord> raw = queue.snapshot();
        List<ChangeRecord> optimized = optimizer.optimize(raw);

        if (optimized.isEmpty()) {
            System.out.println("[SYNC] All queued operations were eliminated by the optimizer.");
            queue.clear();
            return;
        }

        Map<Integer, Integer> studentIdMap = new HashMap<>();
        Map<Integer, Integer> courseIdMap = new HashMap<>();
        Map<Integer, Integer> gradeIdMap = new HashMap<>();

        List<ChangeRecord> remaining = new ArrayList<>();

        System.out.println("[SYNC] Executing " + optimized.size() + " optimized operation(s)...");
        for (ChangeRecord r : optimized) {
            rewriteReferences(r, studentIdMap, courseIdMap, gradeIdMap);

            boolean keepQueued = false;
            try {
                switch (r.getEntityType()) {
                    case STUDENT -> keepQueued = !syncStudent(r, userInput, studentIdMap);
                    case COURSE  -> keepQueued = !syncCourse(r, userInput, courseIdMap);
                    case GRADE   -> keepQueued = !syncGrade(r, userInput, gradeIdMap);
                }
            } catch (Exception e) {
                System.out.println("[SYNC][ERROR] Unexpected error while syncing " + r + " -> " + e.getMessage());
                keepQueued = true;
            }

            if (keepQueued) remaining.add(r);
        }

        queue.replaceWith(remaining);

        System.out.println();
        System.out.println("[SYNC] Done. " + (optimized.size() - remaining.size())
                + " operation(s) successfully sent, "
                + remaining.size() + " kept in queue, "
                + pendingConflicts.size() + " pending conflict(s) awaiting resolution.");
    }

    /**
     * @return true when the record is "done" (either successfully sent or
     *         deliberately abandoned). false means: keep it in the queue.
     */
    private boolean syncStudent(ChangeRecord r, Scanner userInput, Map<Integer, Integer> idMap) {
        String url = remote.studentUrl();
        switch (r.getActionType()) {
            case CREATE -> {
                StudentDTO body = (StudentDTO) r.getPayload();
                Integer tempId = body.getId();
                body.setId(null);
                System.out.println("[SYNC] POST " + url + "  body=" + body);
                StudentDTO created = restTemplate.postForObject(url, body, StudentDTO.class);
                if (created != null) {
                    idMap.put(tempId, created.getId());
                    store.promoteStudentId(tempId, created);
                    System.out.println("[SYNC]   -> created on server with id=" + created.getId());
                }
                return true;
            }
            case UPDATE -> {
                StudentDTO local = (StudentDTO) r.getPayload();
                StudentDTO serverNow = remote.fetchStudent(local.getId());
                if (serverNow == null) {
                    reportDeletionConflict(r, local);
                    return true;
                }
                if (r.getBusinessRule() == BusinessRule.PROMOTE_TO_QUALIFIED
                        && !revalidatePromoteToQualified(r, local, serverNow)) {
                    return true;
                }
                StudentDTO snapshot = store.studentSnapshot(local.getId()).orElse(null);
                if (snapshot != null && !snapshot.fieldsEqual(serverNow)) {
                    if (!askOverwrite("Student", local.getId(), snapshot, serverNow, local, userInput)) {
                        pendingConflicts.add(new PendingConflict(
                                PendingConflict.Kind.MODIFIED_ON_SERVER, r, local, serverNow,
                                "Student #" + local.getId() + " was modified on the server. Overwrite declined by user."));
                        return false;
                    }
                }
                try {
                    System.out.println("[SYNC] PUT " + url + "/" + local.getId() + "  body=" + local);
                    StudentDTO updated = restTemplate.exchange(
                            url + "/" + local.getId(),
                            org.springframework.http.HttpMethod.PUT,
                            new org.springframework.http.HttpEntity<>(local),
                            StudentDTO.class).getBody();
                    if (updated != null) store.refreshStudentSnapshot(updated);
                    System.out.println("[SYNC]   -> updated.");
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        reportDeletionConflict(r, local);
                    } else {
                        throw e;
                    }
                }
                return true;
            }
            case DELETE -> {
                if (r.getBusinessRule() == BusinessRule.PURGE_FAILED_ECTS
                        && !revalidatePurgeFailedEcts(r)) {
                    return true;
                }
                try {
                    System.out.println("[SYNC] DELETE " + url + "/" + r.getEntityId());
                    restTemplate.delete(url + "/" + r.getEntityId());
                    store.dropStudentSnapshot(r.getEntityId());
                    System.out.println("[SYNC]   -> deleted.");
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        reportDeletionConflict(r, null);
                    } else {
                        throw e;
                    }
                }
                return true;
            }
        }
        return true;
    }

    /* =====================================================================
       Business-rule revalidation against fresh server data.

       These guards run JUST before the actual mutation so that an offline
       decision is not blindly applied if the world changed since the rule
       was evaluated locally. They mirror the thresholds used by
       OfflineBusinessLogic / GatewayService.
       ===================================================================== */

    private static final double FAIL_GRADE = 2.0;
    private static final int FAIL_ECTS_THRESHOLD = 10;
    private static final double QUALIFICATION_GPA = 4.75;

    /**
     * Re-checks that the targeted student STILL has at least 10 ECTS of failed
     * grades on the server. If the threshold no longer holds, reports a
     * BUSINESS_RULE_VIOLATED conflict and tells the caller to skip the DELETE.
     *
     * @return true if it's safe to proceed with the delete, false otherwise.
     */
    private boolean revalidatePurgeFailedEcts(ChangeRecord r) {
        Integer studentId = r.getEntityId();
        if (remote.fetchStudent(studentId) == null) {
            // Already gone server-side - let the normal DELETE path handle the 404.
            return true;
        }
        int failed = sumFailedEctsOnServer(studentId);
        System.out.println("[SYNC][RULE] PURGE_FAILED_ECTS revalidation: Student #" + studentId
                + " currently has " + failed + " failed ECTS on the server.");
        if (failed >= FAIL_ECTS_THRESHOLD) {
            return true;
        }
        reportBusinessConflict(r, null,
                "Purge no longer applies: Student #" + studentId + " now has only "
                        + failed + " failed ECTS on the server (< " + FAIL_ECTS_THRESHOLD
                        + "). DELETE aborted.");
        return false;
    }

    /**
     * Re-checks that promoting the targeted student to QUALIFIED is still
     * warranted on the server:
     *   - the server-side grant must still be NOT_QUALIFIED (i.e. no one else
     *     already qualified or granted them), AND
     *   - their server-side weighted GPA must still be &gt;= 4.75.
     * Reports a BUSINESS_RULE_VIOLATED conflict if either check fails.
     *
     * @return true if it's safe to proceed with the PUT, false otherwise.
     */
    private boolean revalidatePromoteToQualified(ChangeRecord r, StudentDTO local, StudentDTO serverNow) {
        Integer studentId = local.getId();
        String serverGrant = serverNow.getGrant();
        if (serverGrant != null && !StudentDTO.GRANT_NOT_QUALIFIED.equalsIgnoreCase(serverGrant)) {
            reportBusinessConflict(r, local,
                    "Promotion no longer applies: Student #" + studentId
                            + " is already '" + serverGrant + "' on the server. PUT aborted.");
            return false;
        }
        Double gpa = recomputeGpaFromServer(studentId);
        System.out.println("[SYNC][RULE] PROMOTE_TO_QUALIFIED revalidation: Student #" + studentId
                + " has server-side GPA = " + (gpa == null ? "n/a" : String.format("%.3f", gpa)) + ".");
        if (gpa == null || gpa < QUALIFICATION_GPA) {
            reportBusinessConflict(r, local,
                    "Promotion no longer applies: Student #" + studentId
                            + " has server-side GPA = " + (gpa == null ? "n/a" : String.format("%.3f", gpa))
                            + " (< " + QUALIFICATION_GPA + "). PUT aborted.");
            return false;
        }
        return true;
    }

    /** Sum of ECTS over all server-side 2.0 grades for the given student. */
    private int sumFailedEctsOnServer(Integer studentId) {
        List<GradeDTO> grades = remote.fetchGradesForStudent(studentId);
        if (grades.isEmpty()) return 0;
        int total = 0;
        Map<Integer, Integer> ectsCache = new HashMap<>();
        for (GradeDTO g : grades) {
            if (g.getGrade() == null || g.getGrade() != FAIL_GRADE) continue;
            Integer courseId = g.getCourseId();
            if (courseId == null) continue;
            Integer ects = ectsCache.computeIfAbsent(courseId, cid -> {
                CourseDTO c = remote.fetchCourse(cid);
                return (c == null || c.getEcts() == null) ? 0 : c.getEcts();
            });
            total += ects;
        }
        return total;
    }

    /** ECTS-weighted GPA recomputed strictly from fresh server data. */
    private Double recomputeGpaFromServer(Integer studentId) {
        List<GradeDTO> grades = remote.fetchGradesForStudent(studentId);
        if (grades.isEmpty()) return null;
        double weighted = 0.0;
        int totalEcts = 0;
        Map<Integer, Integer> ectsCache = new HashMap<>();
        for (GradeDTO g : grades) {
            if (g.getGrade() == null || g.getCourseId() == null) continue;
            Integer ects = ectsCache.computeIfAbsent(g.getCourseId(), cid -> {
                CourseDTO c = remote.fetchCourse(cid);
                return (c == null || c.getEcts() == null) ? 0 : c.getEcts();
            });
            if (ects == 0) continue;
            weighted += g.getGrade() * ects;
            totalEcts += ects;
        }
        return totalEcts == 0 ? null : weighted / totalEcts;
    }

    private boolean syncCourse(ChangeRecord r, Scanner userInput, Map<Integer, Integer> idMap) {
        String url = remote.courseUrl();
        switch (r.getActionType()) {
            case CREATE -> {
                CourseDTO body = (CourseDTO) r.getPayload();
                Integer tempId = body.getId();
                body.setId(null);
                System.out.println("[SYNC] POST " + url + "  body=" + body);
                CourseDTO created = restTemplate.postForObject(url, body, CourseDTO.class);
                if (created != null) {
                    idMap.put(tempId, created.getId());
                    store.promoteCourseId(tempId, created);
                    System.out.println("[SYNC]   -> created on server with id=" + created.getId());
                }
                return true;
            }
            case UPDATE -> {
                CourseDTO local = (CourseDTO) r.getPayload();
                CourseDTO serverNow = remote.fetchCourse(local.getId());
                if (serverNow == null) {
                    reportDeletionConflict(r, local);
                    return true;
                }
                CourseDTO snapshot = store.courseSnapshot(local.getId()).orElse(null);
                if (snapshot != null && !snapshot.fieldsEqual(serverNow)) {
                    if (!askOverwrite("Course", local.getId(), snapshot, serverNow, local, userInput)) {
                        pendingConflicts.add(new PendingConflict(
                                PendingConflict.Kind.MODIFIED_ON_SERVER, r, local, serverNow,
                                "Course #" + local.getId() + " was modified on the server. Overwrite declined by user."));
                        return false;
                    }
                }
                try {
                    System.out.println("[SYNC] PUT " + url + "/" + local.getId() + "  body=" + local);
                    CourseDTO updated = restTemplate.exchange(
                            url + "/" + local.getId(),
                            org.springframework.http.HttpMethod.PUT,
                            new org.springframework.http.HttpEntity<>(local),
                            CourseDTO.class).getBody();
                    if (updated != null) store.refreshCourseSnapshot(updated);
                    System.out.println("[SYNC]   -> updated.");
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        reportDeletionConflict(r, local);
                    } else {
                        throw e;
                    }
                }
                return true;
            }
            case DELETE -> {
                try {
                    System.out.println("[SYNC] DELETE " + url + "/" + r.getEntityId());
                    restTemplate.delete(url + "/" + r.getEntityId());
                    store.dropCourseSnapshot(r.getEntityId());
                    System.out.println("[SYNC]   -> deleted.");
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        reportDeletionConflict(r, null);
                    } else {
                        throw e;
                    }
                }
                return true;
            }
        }
        return true;
    }

    private boolean syncGrade(ChangeRecord r, Scanner userInput, Map<Integer, Integer> idMap) {
        String url = remote.gradeUrl();
        switch (r.getActionType()) {
            case CREATE -> {
                GradeDTO body = (GradeDTO) r.getPayload();
                Integer tempId = body.getId();

                if (body.getStudentId() != null && body.getStudentId() < 0) {
                    reportBusinessConflict(r, body,
                            "Cannot POST Grade: it references a student (tempId=" + body.getStudentId() + ") that was not synced successfully.");
                    return true;
                }
                if (body.getCourseId() != null && body.getCourseId() < 0) {
                    reportBusinessConflict(r, body,
                            "Cannot POST Grade: it references a course (tempId=" + body.getCourseId() + ") that was not synced successfully.");
                    return true;
                }
                if (remote.fetchStudent(body.getStudentId()) == null) {
                    reportBusinessConflict(r, body,
                            "Cannot POST Grade: referenced Student id=" + body.getStudentId()
                                    + " no longer exists on the server (was deleted by someone else).");
                    return true;
                }
                if (remote.fetchCourse(body.getCourseId()) == null) {
                    reportBusinessConflict(r, body,
                            "Cannot POST Grade: referenced Course id=" + body.getCourseId()
                                    + " no longer exists on the server (was deleted by someone else).");
                    return true;
                }

                body.setId(null);
                System.out.println("[SYNC] POST " + url + "  body=" + body);
                GradeDTO created = restTemplate.postForObject(url, body, GradeDTO.class);
                if (created != null) {
                    idMap.put(tempId, created.getId());
                    store.promoteGradeId(tempId, created);
                    System.out.println("[SYNC]   -> created on server with id=" + created.getId());
                }
                return true;
            }
            case UPDATE -> {
                GradeDTO local = (GradeDTO) r.getPayload();
                GradeDTO serverNow = remote.fetchGrade(local.getId());
                if (serverNow == null) {
                    reportDeletionConflict(r, local);
                    return true;
                }
                GradeDTO snapshot = store.gradeSnapshot(local.getId()).orElse(null);
                if (snapshot != null && !snapshot.fieldsEqual(serverNow)) {
                    if (!askOverwrite("Grade", local.getId(), snapshot, serverNow, local, userInput)) {
                        pendingConflicts.add(new PendingConflict(
                                PendingConflict.Kind.MODIFIED_ON_SERVER, r, local, serverNow,
                                "Grade #" + local.getId() + " was modified on the server. Overwrite declined by user."));
                        return false;
                    }
                }
                try {
                    System.out.println("[SYNC] PUT " + url + "/" + local.getId() + "  body=" + local);
                    GradeDTO updated = restTemplate.exchange(
                            url + "/" + local.getId(),
                            org.springframework.http.HttpMethod.PUT,
                            new org.springframework.http.HttpEntity<>(local),
                            GradeDTO.class).getBody();
                    if (updated != null) store.refreshGradeSnapshot(updated);
                    System.out.println("[SYNC]   -> updated.");
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        reportDeletionConflict(r, local);
                    } else {
                        throw e;
                    }
                }
                return true;
            }
            case DELETE -> {
                try {
                    System.out.println("[SYNC] DELETE " + url + "/" + r.getEntityId());
                    restTemplate.delete(url + "/" + r.getEntityId());
                    store.dropGradeSnapshot(r.getEntityId());
                    System.out.println("[SYNC]   -> deleted.");
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        reportDeletionConflict(r, null);
                    } else {
                        throw e;
                    }
                }
                return true;
            }
        }
        return true;
    }

    /**
     * After a CREATE returns a real id we must rewrite any subsequent records
     * that still reference the now-obsolete temporary id (in particular, Grade
     * records whose studentId / courseId was assigned offline).
     */
    private void rewriteReferences(ChangeRecord r,
                                   Map<Integer, Integer> studentIdMap,
                                   Map<Integer, Integer> courseIdMap,
                                   Map<Integer, Integer> gradeIdMap) {
        if (r.getEntityType() == EntityType.GRADE && r.getPayload() instanceof GradeDTO g) {
            if (g.getStudentId() != null && studentIdMap.containsKey(g.getStudentId())) {
                Integer newId = studentIdMap.get(g.getStudentId());
                System.out.println("[SYNC] Rewriting Grade.studentId " + g.getStudentId() + " -> " + newId);
                g.setStudentId(newId);
            }
            if (g.getCourseId() != null && courseIdMap.containsKey(g.getCourseId())) {
                Integer newId = courseIdMap.get(g.getCourseId());
                System.out.println("[SYNC] Rewriting Grade.courseId " + g.getCourseId() + " -> " + newId);
                g.setCourseId(newId);
            }
        }
        if (r.getEntityId() != null) {
            Map<Integer, Integer> map = switch (r.getEntityType()) {
                case STUDENT -> studentIdMap;
                case COURSE  -> courseIdMap;
                case GRADE   -> gradeIdMap;
            };
            if (map.containsKey(r.getEntityId())) {
                Integer newId = map.get(r.getEntityId());
                System.out.println("[SYNC] Rewriting " + r.getEntityType() + " entityId " + r.getEntityId() + " -> " + newId);
                r.setEntityId(newId);
            }
        }
    }

    private boolean askOverwrite(String entity, Integer id, Object originalSnapshot, Object serverNow, Object localChange, Scanner s) {
        System.out.println();
        System.out.println("***** MODIFICATION CONFLICT on " + entity + " #" + id + " *****");
        System.out.println("  Original (when we last fetched) : " + originalSnapshot);
        System.out.println("  Server NOW                       : " + serverNow);
        System.out.println("  Your local change                : " + localChange);
        System.out.print("  Overwrite the server with your local change? (Y/N): ");
        String answer = s.nextLine().trim();
        return answer.equalsIgnoreCase("Y") || answer.equalsIgnoreCase("YES");
    }

    private void reportDeletionConflict(ChangeRecord r, Object localPayload) {
        String msg = "DELETION CONFLICT: " + r.getEntityType() + " #" + r.getEntityId()
                + " was deleted by someone else on the server (HTTP 404). "
                + "Your offline " + r.getActionType() + " cannot be applied.";
        System.out.println();
        System.out.println("***** " + msg + " *****");
        System.out.println();
        pendingConflicts.add(new PendingConflict(PendingConflict.Kind.DELETED_ON_SERVER, r, localPayload, null, msg));
    }

    private void reportBusinessConflict(ChangeRecord r, Object localPayload, String msg) {
        System.out.println();
        System.out.println("***** BUSINESS-RULE OFFLINE CONFLICT: " + msg + " *****");
        System.out.println();
        pendingConflicts.add(new PendingConflict(PendingConflict.Kind.BUSINESS_RULE_VIOLATED, r, localPayload, null, msg));
    }

}
