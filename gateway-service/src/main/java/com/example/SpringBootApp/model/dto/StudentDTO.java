package com.example.SpringBootApp.model.dto;

import java.util.Objects;

public class StudentDTO {

    /** Allowed values for the {@link #grant} field, mirrored on the server. */
    public static final String GRANT_NOT_QUALIFIED = "NOT_QUALIFIED";
    public static final String GRANT_QUALIFIED     = "QUALIFIED";
    public static final String GRANT_GRANTED       = "GRANTED";

    private Integer id;
    private String firstName;
    private Integer age;
    private String city;
    /** Scholarship grant status: NOT_QUALIFIED / QUALIFIED / GRANTED. */
    private String grant;

    public StudentDTO() {}

    public StudentDTO(Integer id, String firstName, Integer age, String city) {
        this(id, firstName, age, city, GRANT_NOT_QUALIFIED);
    }

    public StudentDTO(Integer id, String firstName, Integer age, String city, String grant) {
        this.id = id;
        this.firstName = firstName;
        this.age = age;
        this.city = city;
        this.grant = grant;
    }

    public StudentDTO copy() {
        return new StudentDTO(id, firstName, age, city, grant);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getGrant() { return grant; }
    public void setGrant(String grant) { this.grant = grant; }

    /**
     * Compares the meaningful business fields (everything except id).
     * Used to detect "modification conflicts" between local and remote snapshots.
     */
    public boolean fieldsEqual(StudentDTO other) {
        if (other == null) return false;
        return Objects.equals(firstName, other.firstName)
                && Objects.equals(age, other.age)
                && Objects.equals(city, other.city)
                && Objects.equals(grant, other.grant);
    }

    @Override
    public String toString() {
        return "Student{id=" + id + ", firstName='" + firstName + "', age=" + age
                + ", city='" + city + "', grant=" + grant + "}";
    }
}
