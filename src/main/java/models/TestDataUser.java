package models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestDataUser {
    @JsonProperty("username") private String username;
    @JsonProperty("firstName") private String firstName;
    @JsonProperty("lastName") private String lastName;
    @JsonProperty("zipCode") private String zipCode;

    // Геттеры для тестов
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getZipCode() { return zipCode; }
}