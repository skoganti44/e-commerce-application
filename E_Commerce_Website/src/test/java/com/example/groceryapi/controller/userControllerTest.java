package com.example.groceryapi.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.groceryapi.model.Users;
import com.example.groceryapi.service.userService;

@WebMvcTest(userController.class)
public class userControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private userService userService;

    @Test
    public void testFetchUsers_ReturnsUserList() throws Exception {
        Users user1 = new Users();
        user1.setuserid(1);
        user1.setname("John");
        user1.setemail("john@example.com");
        user1.setpassword("pass123");
        user1.setcreatedat(LocalDateTime.of(2025, 1, 1, 10, 0));

        Users user2 = new Users();
        user2.setuserid(2);
        user2.setname("Jane");
        user2.setemail("jane@example.com");
        user2.setpassword("pass456");
        user2.setcreatedat(LocalDateTime.of(2025, 2, 1, 12, 0));

        List<Users> userList = Arrays.asList(user1, user2);

        when(userService.fetchUsers()).thenReturn(userList);

        mockMvc.perform(get("/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].name").value("Jane"))
                .andExpect(jsonPath("$[1].email").value("jane@example.com"));
    }

    @Test
    public void testFetchUsers_ReturnsEmptyList() throws Exception {
        when(userService.fetchUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testFetchUsers_WithoutAcceptHeader_Returns406() throws Exception {
        mockMvc.perform(get("/users").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }
}
