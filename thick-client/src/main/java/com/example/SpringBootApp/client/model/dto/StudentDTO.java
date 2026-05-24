package com.example.SpringBootApp.client.model.dto;

import java.util.Objects;

public class StudentDTO {
    private Integer id;
    private String firstName;
    private Integer age;
    private String city;

    public StudentDTO() {}

    public StudentDTO(Integer id, String firstName, Integer age, String city) {
        this.id = id;
        this.firstName = firstName;
        this.age = age;
        this.city = city;
    }

    public StudentDTO copy() {
        return new StudentDTO(id, firstName, age, city);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    /**
     * Compares the meaningful business fields (everything except id).
     * Used to detect "modification conflicts" between local and remote snapshots.
     */
    public boolean fieldsEqual(StudentDTO other) {
        if (other == null) return false;
        return Objects.equals(firstName, other.firstName)
                && Objects.equals(age, other.age)
                && Objects.equals(city, other.city);
    }

    @Override
    public String toString() {
        return "Student{id=" + id + ", firstName='" + firstName + "', age=" + age + ", city='" + city + "'}";
    }
}
