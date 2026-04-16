package com.example.groceryapi.karate;

import com.intuit.karate.junit5.Karate;

public class UsersKarateTest {

    @Karate.Test
    Karate testUsers() {
        return Karate.run("users").relativeTo(getClass());
    }
}
