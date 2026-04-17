package com.example.groceryapi.testdata;

import java.time.LocalDateTime;
import java.util.List;

import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.Users;

public final class TestData {

    private TestData() {}

    public static final LocalDateTime JOHN_CREATED_AT = LocalDateTime.of(2025, 1, 1, 10, 0);
    public static final LocalDateTime JANE_CREATED_AT = LocalDateTime.of(2025, 2, 1, 12, 0);
    public static final LocalDateTime MARCH_CREATED_AT = LocalDateTime.of(2025, 3, 15, 9, 30);

    public static final String FRESH_PRODUCTS = "Fresh Products";
    public static final String FRESH_ORGANIC_PRODUCTS = "Fresh Organic Products";
    public static final String IT = "IT";
    public static final String SALES = "Sales";

    public static Users john() {
        return user(1, "John", "john@example.com", "pass123", JOHN_CREATED_AT);
    }

    public static Users jane() {
        return user(2, "Jane", "jane@example.com", "pass456", JANE_CREATED_AT);
    }

    public static Users johnMarch() {
        return user(1, "John", "john@example.com", "pass123", MARCH_CREATED_AT);
    }

    public static Users newJohn() {
        return user(0, "John", "john@example.com", "pass123", LocalDateTime.now());
    }

    public static Users newJane() {
        return user(0, "Jane", "jane@example.com", "pass456", LocalDateTime.now());
    }

    public static Users user(int id, String name, String email, String password, LocalDateTime createdAt) {
        Users u = new Users();
        u.setuserid(id);
        u.setname(name);
        u.setemail(email);
        u.setpassword(password);
        u.setcreatedat(createdAt);
        return u;
    }

    public static Role alice() {
        return role(1, "Alice Smith", "Manager", SALES);
    }

    public static Role bob() {
        return role(2, "Bob Johnson", "Engineer", IT);
    }

    public static Role joeJonnas() {
        return role(3, "Joe Jonnas", "Manager", FRESH_PRODUCTS);
    }

    public static Role steveWooten() {
        return role(4, "Steve Wooten", "sales Manager", FRESH_PRODUCTS);
    }

    public static Role carol() {
        return role(5, "Carol White", "Lead", FRESH_PRODUCTS);
    }

    public static Role role(int id, String fullName, String roleName, String department) {
        Role r = new Role();
        r.setId(id);
        r.setFullName(fullName);
        r.setRole(roleName);
        r.setDepartment(department);
        return r;
    }

    public static List<Users> users() {
        return List.of(john(), jane());
    }

    public static List<Role> rolesInFreshProducts() {
        return List.of(joeJonnas(), steveWooten());
    }
}
