package com.example.groceryapi.atdd;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.groceryapi.model.User;
import com.example.groceryapi.repository.Repository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private Repository repository;

    @BeforeEach
    public void setup() {
        RestAssured.port = port;
        repository.deleteAllUsers();
    }

    @Test
    @DisplayName("GIVEN users exist WHEN I fetch all users THEN I get a list of users with status 200")
    public void shouldReturnAllUsers() {
        // GIVEN
        User user1 = new User();
        user1.setname("John");
        user1.setemail("john@example.com");
        user1.setpassword("pass123");
        user1.setcreatedat(LocalDateTime.now());
        repository.saveUser(user1);

        User user2 = new User();
        user2.setname("Jane");
        user2.setemail("jane@example.com");
        user2.setpassword("pass456");
        user2.setcreatedat(LocalDateTime.now());
        repository.saveUser(user2);

        // WHEN + THEN
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/users")
        .then()
            .statusCode(200)
            .body("", hasSize(2))
            .body("[0].name", equalTo("John"))
            .body("[0].email", equalTo("john@example.com"))
            .body("[1].name", equalTo("Jane"))
            .body("[1].email", equalTo("jane@example.com"));
    }

    @Test
    @DisplayName("GIVEN no users exist WHEN I fetch all users THEN I get an empty list with status 200")
    public void shouldReturnEmptyListWhenNoUsers() {
        // GIVEN — no users in DB (cleaned in @BeforeEach)

        // WHEN + THEN
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/users")
        .then()
            .statusCode(200)
            .body("", empty());
    }

    @Test
    @DisplayName("GIVEN users exist WHEN I fetch users THEN each user has all required fields")
    public void shouldReturnUsersWithAllFields() {
        // GIVEN
        User user = new User();
        user.setname("John");
        user.setemail("john@example.com");
        user.setpassword("pass123");
        user.setcreatedat(LocalDateTime.of(2025, 1, 1, 10, 0));
        repository.saveUser(user);

        // WHEN + THEN
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/users")
        .then()
            .statusCode(200)
            .body("[0].userid", greaterThan(0))
            .body("[0].name", equalTo("John"))
            .body("[0].email", equalTo("john@example.com"))
            .body("[0].password", equalTo("pass123"))
            .body("[0].createdat", equalTo("2025-01-01T10:00:00"));
    }

    @Test
    @DisplayName("GIVEN wrong Accept header WHEN I fetch users THEN I get 406 Not Acceptable")
    public void shouldReturn406ForWrongAcceptHeader() {
        // WHEN + THEN
        given()
            .accept(ContentType.XML)
        .when()
            .get("/users")
        .then()
            .statusCode(406);
    }
}
