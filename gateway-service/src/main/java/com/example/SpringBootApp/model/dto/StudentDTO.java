package com.example.SpringBootApp.model.dto;

public class StudentDTO {
    private Integer id;
    private String firstName;
    private Integer age;
    private String city;
    private String grant;

    public StudentDTO() {};

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
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
