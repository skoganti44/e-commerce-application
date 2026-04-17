package com.example.groceryapi.controller;

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

import com.example.groceryapi.service.userService;
import com.example.groceryapi.testdata.TestData;

@WebMvcTest(Controller.class)
public class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private userService userService;

    @Test
    public void testFetchUsers_ReturnsUserList() throws Exception {
        when(userService.fetchUsers()).thenReturn(TestData.users());

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

    @Test
    public void testFetchRoles_ReturnsRoleList() throws Exception {
        when(userService.fetchRoles(null)).thenReturn(List.of(TestData.alice(), TestData.bob()));

        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Alice Smith"))
                .andExpect(jsonPath("$[0].role").value("Manager"))
                .andExpect(jsonPath("$[0].department").value(TestData.SALES))
                .andExpect(jsonPath("$[1].fullName").value("Bob Johnson"))
                .andExpect(jsonPath("$[1].role").value("Engineer"))
                .andExpect(jsonPath("$[1].department").value(TestData.IT));
    }

    @Test
    public void testFetchRoles_ReturnsEmptyList() throws Exception {
        when(userService.fetchRoles(null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testFetchRoles_WithoutAcceptHeader_Returns406() throws Exception {
        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    public void testFetchRoles_FilterByDepartment() throws Exception {
        when(userService.fetchRoles(TestData.FRESH_PRODUCTS)).thenReturn(List.of(TestData.joeJonnas()));

        mockMvc.perform(get("/roles").param("department", TestData.FRESH_PRODUCTS).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Joe Jonnas"))
                .andExpect(jsonPath("$[0].department").value(TestData.FRESH_PRODUCTS));
    }
}
