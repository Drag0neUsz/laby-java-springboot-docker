package com.example.SpringBootApp.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Student {

    /** Grant qualification status values. */
    public static final String GRANT_NOT_QUALIFIED = "NOT_QUALIFIED";
    public static final String GRANT_QUALIFIED     = "QUALIFIED";
    public static final String GRANT_GRANTED       = "GRANTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String firstName;
    private int age;
    private String city;

    /**
     * Scholarship grant lifecycle: NOT_QUALIFIED -> QUALIFIED -> GRANTED.
     * Defaults to NOT_QUALIFIED for every newly persisted student.
     */
    @Column(nullable = false)
    private String grant = GRANT_NOT_QUALIFIED;


    public Student() {
    }

    public Integer getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getGrant() {
        return grant;
    }
    public void setGrant(String grant) {
        this.grant = grant;
    }
}