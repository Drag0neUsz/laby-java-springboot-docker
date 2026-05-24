package com.example.SpringBootApp.client.console;

import com.example.SpringBootApp.client.model.dto.CourseDTO;
import com.example.SpringBootApp.client.model.dto.GradeDTO;
import com.example.SpringBootApp.client.model.dto.StudentDTO;
import com.example.SpringBootApp.client.service.OfflineBusinessLogic;
import com.example.SpringBootApp.client.service.RemoteFetchService;
import com.example.SpringBootApp.client.store.LocalDataStore;
import com.example.SpringBootApp.client.sync.ActionQueue;
import com.example.SpringBootApp.client.sync.ActionType;
import com.example.SpringBootApp.client.sync.BusinessRule;
import com.example.SpringBootApp.client.sync.ChangeRecord;
import com.example.SpringBootApp.client.sync.EntityType;
import com.example.SpringBootApp.client.sync.PendingConflict;
import com.example.SpringBootApp.client.sync.SyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Interactive console UI exposing the six menu options required by the spec.
 *
 *  a) Fetch data from servers
 *  b) View local state
 *  c) Execute local offline operation
 *  d) View pending changes (show the current sync queue)
 *  e) Synchronize data with servers
 *  f) View and resolve sync conflicts
 *
 * Every CRUD operation invoked from option (c) ONLY mutates the
 * LocalDataStore and pushes a ChangeRecord onto the ActionQueue - no
 * REST calls are made until option (e) is invoked.
 */
@Component
public class ThickClientConsole implements CommandLineRunner {

    /** Allowed grade values according to the Polish academic scale. */
    private static final Set<Double> ALLOWED_GRADES =
            Set.of(2.0, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5);

    /** Inclusive lower bound for a plausible student age. */
    private static final int MIN_AGE = 1;
    /** Inclusive upper bound for a plausible student age. */
    private static final int MAX_AGE = 123;

    private final LocalDataStore store;
    private final ActionQueue queue;
    private final RemoteFetchService fetcher;
    private final SyncService syncService;
    private final OfflineBusinessLogic businessLogic;

    public ThickClientConsole(LocalDataStore store,
                              ActionQueue queue,
                              RemoteFetchService fetcher,
                              SyncService syncService,
                              OfflineBusinessLogic businessLogic) {
        this.store = store;
        this.queue = queue;
        this.fetcher = fetcher;
        this.syncService = syncService;
        this.businessLogic = businessLogic;
    }

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        printBanner();
        while (true) {
            printMenu();
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim().toLowerCase();
            try {
                switch (choice) {
                    case "a" -> fetcher.fetchAll();
                    case "b" -> viewLocalState();
                    case "c" -> offlineOperationsMenu(scanner);
                    case "d" -> viewPendingChanges();
                    case "e" -> syncService.synchronize(scanner);
                    case "f" -> resolveConflictsMenu(scanner);
                    case "q", "quit", "exit" -> {
                        System.out.println("Bye!");
                        return;
                    }
                    default -> System.out.println("Unknown option: '" + choice + "'");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void printBanner() {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println("  THICK CLIENT  -  Distributed Systems offline-first demo");
        System.out.println("  Talks to: students@8081, courses@8082, grades@8083");
        System.out.println("=========================================================");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("------------------- MENU -------------------");
        System.out.println("  a) Fetch data from servers");
        System.out.println("  b) View local state");
        System.out.println("  c) Execute local offline operation");
        System.out.println("  d) View pending changes (sync queue)");
        System.out.println("  e) Synchronize data with servers");
        System.out.println("  f) View and resolve sync conflicts");
        System.out.println("  q) Quit");
        System.out.println("--------------------------------------------");
    }

    private void viewLocalState() {
        System.out.println();
        System.out.println("---------- LOCAL STATE ----------");
        List<StudentDTO> students = store.allStudents();
        List<CourseDTO> courses = store.allCourses();
        List<GradeDTO> grades = store.allGrades();

        System.out.println("Students (" + students.size() + "):");
        students.forEach(s -> System.out.println("  " + s));
        System.out.println("Courses (" + courses.size() + "):");
        courses.forEach(c -> System.out.println("  " + c));
        System.out.println("Grades (" + grades.size() + "):");
        grades.forEach(g -> System.out.println("  " + g));
        System.out.println("---------------------------------");
    }

    private void viewPendingChanges() {
        System.out.println();
        System.out.println("---------- ACTION QUEUE / CHANGELOG ----------");
        List<ChangeRecord> records = queue.snapshot();
        if (records.isEmpty()) {
            System.out.println("(empty - no pending changes)");
        } else {
            System.out.println("Pending operations: " + records.size());
            for (ChangeRecord r : records) {
                System.out.println("  " + r);
            }
        }
        System.out.println("----------------------------------------------");
    }

    /* ============================================================
       OFFLINE OPERATIONS (menu option c)
       Every action mutates LocalDataStore and appends a ChangeRecord.
       NOTHING here calls a REST endpoint.
       ============================================================ */

    private void offlineOperationsMenu(Scanner s) {
        while (true) {
            System.out.println();
            System.out.println("------- OFFLINE OPERATIONS (local only) -------");
            System.out.println(" 1) Create student");
            System.out.println(" 2) Update student");
            System.out.println(" 3) Delete student");
            System.out.println(" 4) Create course");
            System.out.println(" 5) Update course");
            System.out.println(" 6) Delete course");
            System.out.println(" 7) Create grade");
            System.out.println(" 8) Update grade");
            System.out.println(" 9) Delete grade");
            System.out.println("10) [BIZ] Compute student's weighted GPA (offline)");
            System.out.println("11) [BIZ] Top students by weighted GPA (offline)");
            System.out.println("12) [BIZ] Count failed grades for a course (offline)");
            System.out.println("13) [BIZ] List courses attended by student (offline)");
            System.out.println("14) [BIZ] Purge students with >=10 failed ECTS (offline)");
            System.out.println("15) [BIZ] Promote NOT_QUALIFIED students with GPA >= 4.75 (offline)");
            System.out.println(" 0) Back");
            System.out.println("------------------------------------------------");
            System.out.print("Choose: ");
            String c = s.nextLine().trim();
            switch (c) {
                case "1" -> createStudent(s);
                case "2" -> updateStudent(s);
                case "3" -> deleteStudent(s);
                case "4" -> createCourse(s);
                case "5" -> updateCourse(s);
                case "6" -> deleteCourse(s);
                case "7" -> createGrade(s);
                case "8" -> updateGrade(s);
                case "9" -> deleteGrade(s);
                case "10" -> computeGpa(s);
                case "11" -> topStudents(s);
                case "12" -> failedCount(s);
                case "13" -> coursesAttended(s);
                case "14" -> purgeFailedStudents(s);
                case "15" -> promoteQualifiedStudents(s);
                case "0" -> { return; }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private void createStudent(Scanner s) {
        System.out.print("First name: ");
        String firstName = s.nextLine().trim();
        if (firstName.isEmpty()) {
            System.out.println("[LOCAL][ERROR] First name is required and cannot be empty.");
            return;
        }
        System.out.print("Age       : ");
        String ageStr = s.nextLine().trim();
        if (ageStr.isEmpty()) {
            System.out.println("[LOCAL][ERROR] Age is required.");
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            System.out.println("[LOCAL][ERROR] Age must be an integer.");
            return;
        }
        if (age < MIN_AGE || age > MAX_AGE) {
            System.out.println("[LOCAL][ERROR] Age must be in range " + MIN_AGE + ".." + MAX_AGE + " (got " + age + ").");
            return;
        }
        System.out.print("City      : ");
        String city = s.nextLine().trim();

        int tempId = store.nextTempId();
        StudentDTO local = new StudentDTO(tempId, firstName, age, city, StudentDTO.GRANT_NOT_QUALIFIED);
        store.putStudent(local);
        queue.record(new ChangeRecord(EntityType.STUDENT, ActionType.CREATE, tempId, local.copy()));
        System.out.println("[LOCAL] Student created with TEMP id=" + tempId + " -> " + local);
    }

    private void updateStudent(Scanner s) {
        System.out.print("Student id to update: ");
        int id = Integer.parseInt(s.nextLine().trim());
        StudentDTO existing = store.findStudent(id).orElse(null);
        if (existing == null) {
            System.out.println("[LOCAL] No student with id=" + id + " in local state.");
            return;
        }
        System.out.print("New first name [" + existing.getFirstName() + "]: ");
        String fn = s.nextLine().trim();
        System.out.print("New age        [" + existing.getAge() + "]: ");
        String ageStr = s.nextLine().trim();
        System.out.print("New city       [" + existing.getCity() + "]: ");
        String city = s.nextLine().trim();
        System.out.print("New grant      [" + existing.getGrant()
                + "] (NOT_QUALIFIED / QUALIFIED / GRANTED, blank to keep): ");
        String grant = s.nextLine().trim();

        Integer newAge = null;
        if (!ageStr.isEmpty()) {
            try {
                newAge = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                System.out.println("[LOCAL][ERROR] Age must be an integer.");
                return;
            }
            if (newAge < MIN_AGE || newAge > MAX_AGE) {
                System.out.println("[LOCAL][ERROR] Age must be in range " + MIN_AGE + ".." + MAX_AGE + " (got " + newAge + ").");
                return;
            }
        }
        if (!grant.isEmpty() && !isValidGrant(grant)) {
            System.out.println("[LOCAL][ERROR] Grant must be NOT_QUALIFIED, QUALIFIED or GRANTED (got '" + grant + "').");
            return;
        }

        if (!fn.isEmpty()) existing.setFirstName(fn);
        if (newAge != null) existing.setAge(newAge);
        if (!city.isEmpty()) existing.setCity(city);
        if (!grant.isEmpty()) existing.setGrant(grant.toUpperCase());

        queue.record(new ChangeRecord(EntityType.STUDENT, ActionType.UPDATE, id, existing.copy()));
        System.out.println("[LOCAL] Student updated -> " + existing);
    }

    private static boolean isValidGrant(String value) {
        if (value == null) return false;
        String v = value.trim().toUpperCase();
        return v.equals(StudentDTO.GRANT_NOT_QUALIFIED)
                || v.equals(StudentDTO.GRANT_QUALIFIED)
                || v.equals(StudentDTO.GRANT_GRANTED);
    }

    private void deleteStudent(Scanner s) {
        System.out.print("Student id to delete: ");
        int id = Integer.parseInt(s.nextLine().trim());
        if (store.findStudent(id).isEmpty()) {
            System.out.println("[LOCAL] No student with id=" + id + " in local state.");
            return;
        }
        store.removeStudent(id);
        queue.record(new ChangeRecord(EntityType.STUDENT, ActionType.DELETE, id, null));
        System.out.println("[LOCAL] Student id=" + id + " marked for deletion.");
    }

    private void createCourse(Scanner s) {
        System.out.print("Name: ");
        String name = s.nextLine().trim();
        System.out.print("ECTS: ");
        int ects = Integer.parseInt(s.nextLine().trim());

        int tempId = store.nextTempId();
        CourseDTO local = new CourseDTO(tempId, name, ects);
        store.putCourse(local);
        queue.record(new ChangeRecord(EntityType.COURSE, ActionType.CREATE, tempId, local.copy()));
        System.out.println("[LOCAL] Course created with TEMP id=" + tempId + " -> " + local);
    }

    private void updateCourse(Scanner s) {
        System.out.print("Course id to update: ");
        int id = Integer.parseInt(s.nextLine().trim());
        CourseDTO existing = store.findCourse(id).orElse(null);
        if (existing == null) {
            System.out.println("[LOCAL] No course with id=" + id + " in local state.");
            return;
        }
        System.out.print("New name [" + existing.getName() + "]: ");
        String name = s.nextLine().trim();
        System.out.print("New ects [" + existing.getEcts() + "]: ");
        String ectsStr = s.nextLine().trim();

        if (!name.isEmpty()) existing.setName(name);
        if (!ectsStr.isEmpty()) existing.setEcts(Integer.parseInt(ectsStr));

        queue.record(new ChangeRecord(EntityType.COURSE, ActionType.UPDATE, id, existing.copy()));
        System.out.println("[LOCAL] Course updated -> " + existing);
    }

    private void deleteCourse(Scanner s) {
        System.out.print("Course id to delete: ");
        int id = Integer.parseInt(s.nextLine().trim());
        if (store.findCourse(id).isEmpty()) {
            System.out.println("[LOCAL] No course with id=" + id + " in local state.");
            return;
        }
        store.removeCourse(id);
        queue.record(new ChangeRecord(EntityType.COURSE, ActionType.DELETE, id, null));
        System.out.println("[LOCAL] Course id=" + id + " marked for deletion.");
    }

    private void createGrade(Scanner s) {
        System.out.print("Student id (may be a local TEMP negative id): ");
        String sidStr = s.nextLine().trim();
        int sid;
        try {
            sid = Integer.parseInt(sidStr);
        } catch (NumberFormatException e) {
            System.out.println("[LOCAL][ERROR] Student id must be an integer.");
            return;
        }
        if (store.findStudent(sid).isEmpty()) {
            System.out.println("[LOCAL][ERROR] No local student with id=" + sid
                    + ". Fetch from server (option a) or create the student first.");
            return;
        }

        System.out.print("Course id  (may be a local TEMP negative id): ");
        String cidStr = s.nextLine().trim();
        int cid;
        try {
            cid = Integer.parseInt(cidStr);
        } catch (NumberFormatException e) {
            System.out.println("[LOCAL][ERROR] Course id must be an integer.");
            return;
        }
        if (store.findCourse(cid).isEmpty()) {
            System.out.println("[LOCAL][ERROR] No local course with id=" + cid
                    + ". Fetch from server (option a) or create the course first.");
            return;
        }

        System.out.print("Grade (one of " + ALLOWED_GRADES + "): ");
        String gStr = s.nextLine().trim();
        double g;
        try {
            g = Double.parseDouble(gStr);
        } catch (NumberFormatException e) {
            System.out.println("[LOCAL][ERROR] Grade must be a number.");
            return;
        }
        if (!ALLOWED_GRADES.contains(g)) {
            System.out.println("[LOCAL][ERROR] Grade " + g + " is not allowed. Must be one of " + ALLOWED_GRADES + ".");
            return;
        }

        int tempId = store.nextTempId();
        GradeDTO local = new GradeDTO(tempId, g, sid, cid);
        store.putGrade(local);
        queue.record(new ChangeRecord(EntityType.GRADE, ActionType.CREATE, tempId, local.copy()));
        System.out.println("[LOCAL] Grade created with TEMP id=" + tempId + " -> " + local);
    }

    private void updateGrade(Scanner s) {
        System.out.print("Grade id to update: ");
        int id = Integer.parseInt(s.nextLine().trim());
        GradeDTO existing = store.findGrade(id).orElse(null);
        if (existing == null) {
            System.out.println("[LOCAL] No grade with id=" + id + " in local state.");
            return;
        }
        System.out.print("New grade value [" + existing.getGrade() + "] (one of " + ALLOWED_GRADES + "): ");
        String gStr = s.nextLine().trim();
        if (!gStr.isEmpty()) {
            double newGrade;
            try {
                newGrade = Double.parseDouble(gStr);
            } catch (NumberFormatException e) {
                System.out.println("[LOCAL][ERROR] Grade must be a number.");
                return;
            }
            if (!ALLOWED_GRADES.contains(newGrade)) {
                System.out.println("[LOCAL][ERROR] Grade " + newGrade + " is not allowed. Must be one of " + ALLOWED_GRADES + ".");
                return;
            }
            existing.setGrade(newGrade);
        }

        queue.record(new ChangeRecord(EntityType.GRADE, ActionType.UPDATE, id, existing.copy()));
        System.out.println("[LOCAL] Grade updated -> " + existing);
    }

    private void deleteGrade(Scanner s) {
        System.out.print("Grade id to delete: ");
        int id = Integer.parseInt(s.nextLine().trim());
        if (store.findGrade(id).isEmpty()) {
            System.out.println("[LOCAL] No grade with id=" + id + " in local state.");
            return;
        }
        store.removeGrade(id);
        queue.record(new ChangeRecord(EntityType.GRADE, ActionType.DELETE, id, null));
        System.out.println("[LOCAL] Grade id=" + id + " marked for deletion.");
    }

    private void computeGpa(Scanner s) {
        System.out.print("Student id: ");
        int id = Integer.parseInt(s.nextLine().trim());
        Double gpa = businessLogic.weightedGpa(id);
        if (gpa == null) {
            System.out.println("[BIZ] No grades for student id=" + id + " in the local store - cannot compute GPA.");
        } else {
            System.out.printf("[BIZ] Weighted GPA for student %d = %.3f%n", id, gpa);
        }
    }

    private void topStudents(Scanner s) {
        System.out.print("GPA threshold (e.g. 4.5): ");
        double threshold = Double.parseDouble(s.nextLine().trim());
        List<Map.Entry<StudentDTO, Double>> top = businessLogic.topStudents(threshold);
        if (top.isEmpty()) {
            System.out.println("[BIZ] No students with weighted GPA >= " + threshold + " in the local store.");
            return;
        }
        System.out.println("[BIZ] Top students (offline computation):");
        top.forEach(e -> System.out.printf("  %.3f  -  %s%n", e.getValue(), e.getKey()));
    }

    private void failedCount(Scanner s) {
        System.out.print("Course id: ");
        int id = Integer.parseInt(s.nextLine().trim());
        long count = businessLogic.failedCount(id);
        System.out.println("[BIZ] Failed grades for course " + id + " (offline): " + count);
    }

    private void coursesAttended(Scanner s) {
        System.out.print("Student id: ");
        int id = Integer.parseInt(s.nextLine().trim());
        List<String> names = businessLogic.coursesAttendedBy(id);
        if (names.isEmpty()) {
            System.out.println("[BIZ] Student " + id + " has no grades for any course in the local store.");
            return;
        }
        System.out.println("[BIZ] Courses attended by student " + id + " (offline):");
        names.forEach(n -> System.out.println("  - " + n));
    }

    /**
     * Locally delete every student whose 2.0 grades sum to >= 10 ECTS and
     * push a DELETE entry per student onto the sync queue. Asks for
     * confirmation before mutating local state.
     */
    private void purgeFailedStudents(Scanner s) {
        List<Map.Entry<StudentDTO, Integer>> candidates = businessLogic.studentsToPurgeForFailedEcts();
        if (candidates.isEmpty()) {
            System.out.println("[BIZ] No students reach the "
                    + OfflineBusinessLogic.FAIL_ECTS_THRESHOLD + "-ECTS failure threshold in the local store.");
            return;
        }
        System.out.println("[BIZ] Students that would be PURGED (>= "
                + OfflineBusinessLogic.FAIL_ECTS_THRESHOLD + " failed ECTS, offline view):");
        for (Map.Entry<StudentDTO, Integer> e : candidates) {
            System.out.printf("  failedEcts=%d  -  %s%n", e.getValue(), e.getKey());
        }
        System.out.print("Apply purge locally and queue DELETE for each? (Y/N): ");
        String answer = s.nextLine().trim();
        if (!answer.equalsIgnoreCase("Y") && !answer.equalsIgnoreCase("YES")) {
            System.out.println("[BIZ] Purge cancelled.");
            return;
        }
        int deleted = 0;
        for (Map.Entry<StudentDTO, Integer> e : candidates) {
            Integer sid = e.getKey().getId();
            store.removeStudent(sid);
            ChangeRecord cr = new ChangeRecord(EntityType.STUDENT, ActionType.DELETE, sid, null);
            cr.setBusinessRule(BusinessRule.PURGE_FAILED_ECTS);
            queue.record(cr);
            deleted++;
        }
        System.out.println("[LOCAL] Purged " + deleted + " student(s); each DELETE queued for next sync"
                + " (will be revalidated against the server before being applied).");
    }

    /**
     * Locally promote every NOT_QUALIFIED student with weighted GPA >= 4.75
     * to QUALIFIED and queue an UPDATE per student. Asks for confirmation
     * before mutating local state.
     */
    private void promoteQualifiedStudents(Scanner s) {
        List<Map.Entry<StudentDTO, Double>> candidates = businessLogic.studentsToPromoteToQualified();
        if (candidates.isEmpty()) {
            System.out.println("[BIZ] No NOT_QUALIFIED students reach the GPA threshold of "
                    + OfflineBusinessLogic.QUALIFICATION_GPA + " in the local store.");
            return;
        }
        System.out.println("[BIZ] Students that would be PROMOTED to QUALIFIED (offline view):");
        for (Map.Entry<StudentDTO, Double> e : candidates) {
            System.out.printf("  gpa=%.3f  -  %s%n", e.getValue(), e.getKey());
        }
        System.out.print("Apply promotion locally and queue UPDATE for each? (Y/N): ");
        String answer = s.nextLine().trim();
        if (!answer.equalsIgnoreCase("Y") && !answer.equalsIgnoreCase("YES")) {
            System.out.println("[BIZ] Promotion cancelled.");
            return;
        }
        int promoted = 0;
        for (Map.Entry<StudentDTO, Double> e : candidates) {
            StudentDTO stud = e.getKey();
            stud.setGrant(StudentDTO.GRANT_QUALIFIED);
            ChangeRecord cr = new ChangeRecord(EntityType.STUDENT, ActionType.UPDATE, stud.getId(), stud.copy());
            cr.setBusinessRule(BusinessRule.PROMOTE_TO_QUALIFIED);
            queue.record(cr);
            promoted++;
        }
        System.out.println("[LOCAL] Promoted " + promoted + " student(s); each UPDATE queued for next sync"
                + " (will be revalidated against the server before being applied).");
    }

    /* ============================================================
       CONFLICT VIEW & RESOLUTION (menu option f)
       ============================================================ */

    private void resolveConflictsMenu(Scanner s) {
        while (true) {
            List<PendingConflict> conflicts = syncService.getPendingConflicts();
            System.out.println();
            System.out.println("---------- PENDING SYNC CONFLICTS ----------");
            if (conflicts.isEmpty()) {
                System.out.println("(no pending conflicts)");
                return;
            }
            for (int i = 0; i < conflicts.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + conflicts.get(i));
            }
            System.out.println("--------------------------------------------");
            System.out.println(" r <n>  Resolve conflict #n");
            System.out.println(" c      Clear ALL conflicts");
            System.out.println(" 0      Back");
            System.out.print("Choose: ");
            String line = s.nextLine().trim();
            if (line.equals("0")) return;
            if (line.equals("c")) {
                syncService.clearPendingConflicts();
                System.out.println("[CONFLICT] All pending conflicts cleared.");
                continue;
            }
            if (line.startsWith("r ")) {
                try {
                    int idx = Integer.parseInt(line.substring(2).trim()) - 1;
                    if (idx < 0 || idx >= conflicts.size()) {
                        System.out.println("Invalid conflict number.");
                        continue;
                    }
                    resolveSingleConflict(conflicts.get(idx), s);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number.");
                }
                continue;
            }
            System.out.println("Unknown command.");
        }
    }

    private void resolveSingleConflict(PendingConflict pc, Scanner s) {
        System.out.println();
        System.out.println("Resolving: " + pc);
        switch (pc.getKind()) {
            case DELETED_ON_SERVER -> {
                System.out.println("Options: (d)iscard local change   (b)ack");
                System.out.print("Choose: ");
                String c = s.nextLine().trim();
                if (c.equalsIgnoreCase("d")) {
                    dropChangeFromQueue(pc.getRecord());
                    removeFromConflicts(pc);
                    System.out.println("[CONFLICT] Local change discarded.");
                }
            }
            case MODIFIED_ON_SERVER -> {
                System.out.println("Local change : " + pc.getLocalPayload());
                System.out.println("Server now   : " + pc.getServerPayload());
                System.out.println("Options: (o)verwrite server with local on next sync"
                        + "   (d)iscard local change   (b)ack");
                System.out.print("Choose: ");
                String c = s.nextLine().trim();
                if (c.equalsIgnoreCase("o")) {
                    refreshSnapshotForOverwrite(pc);
                    removeFromConflicts(pc);
                    System.out.println("[CONFLICT] Snapshot refreshed; the queued PUT will now overwrite the server on next sync.");
                } else if (c.equalsIgnoreCase("d")) {
                    dropChangeFromQueue(pc.getRecord());
                    removeFromConflicts(pc);
                    System.out.println("[CONFLICT] Local change discarded.");
                }
            }
            case BUSINESS_RULE_VIOLATED -> {
                System.out.println("Options: (d)iscard local change   (b)ack");
                System.out.print("Choose: ");
                String c = s.nextLine().trim();
                if (c.equalsIgnoreCase("d")) {
                    dropChangeFromQueue(pc.getRecord());
                    removeFromConflicts(pc);
                    System.out.println("[CONFLICT] Local change discarded.");
                }
            }
        }
    }

    /**
     * Overwriting requires us to "agree" with the current server state - by
     * pretending that's what we last fetched, the next sync will not flag the
     * record as a modification conflict and will simply PUT.
     */
    @SuppressWarnings("unchecked")
    private void refreshSnapshotForOverwrite(PendingConflict pc) {
        Object server = pc.getServerPayload();
        if (server instanceof StudentDTO sv) store.refreshStudentSnapshot(sv);
        else if (server instanceof CourseDTO cv) store.refreshCourseSnapshot(cv);
        else if (server instanceof GradeDTO gv) store.refreshGradeSnapshot(gv);
    }

    private void dropChangeFromQueue(ChangeRecord r) {
        List<ChangeRecord> remaining = queue.snapshot().stream()
                .filter(x -> x.getSequence() != r.getSequence())
                .toList();
        queue.replaceWith(remaining);
    }

    private void removeFromConflicts(PendingConflict pc) {
        syncService.removePendingConflict(pc);
    }
}
