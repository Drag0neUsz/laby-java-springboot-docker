package com.example.SpringBootApp.client.sync;

/**
 * Marks a change record as the by-product of a thick-client business rule
 * that must be revalidated against fresh server state before it is allowed
 * to mutate the repository.
 *
 * Plain CRUD operations carry {@link #NONE}; the two batch operations exposed
 * by the offline console use the dedicated tags below.
 */
public enum BusinessRule {

    /** Ordinary CRUD change - no extra precondition to check at sync time. */
    NONE,

    /**
     * The associated DELETE STUDENT record was produced by the "purge students
     * with &gt;=10 failed ECTS" operation. The sync layer must re-fetch the
     * student's grades + courses and only proceed if the threshold is still
     * met server-side.
     */
    PURGE_FAILED_ECTS,

    /**
     * The associated UPDATE STUDENT record was produced by the "promote
     * NOT_QUALIFIED students with GPA &gt;= 4.75" operation. The sync layer
     * must verify both:
     *   1. the server-side grant is still NOT_QUALIFIED, and
     *   2. the recomputed weighted GPA is still &gt;= 4.75.
     */
    PROMOTE_TO_QUALIFIED
}
