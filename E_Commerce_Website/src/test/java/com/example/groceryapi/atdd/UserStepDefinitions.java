package com.example.groceryapi.atdd;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.groceryapi.model.Users;
import com.example.groceryapi.repository.Repository;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class UserStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Repository repository;

    private ResultActions resultActions;

    @Before
    public void beforeEach() {
        repository.deleteAllUsers();
    }

    @After
    public void cleanup() {
        repository.deleteAllUsers();
    }

    @Given("the following users exist in the system")
    public void theFollowingUsersExist(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        for (Map<String, String> row : rows) {
            Users user = new Users();
            user.setname(row.get("name"));
            user.setemail(row.get("email"));
            user.setpassword(row.get("password"));
            user.setcreatedat(LocalDateTime.now());
            repository.saveUser(user);
        }
    }

    @Given("no users exist in the system")
    public void noUsersExist() {
        repository.deleteAllUsers();
    }

    @When("I send a GET request to {string} with Accept header {string}")
    public void iSendGetRequest(String url, String acceptHeader) throws Exception {
        MediaType mediaType = acceptHeader.equals("application/json")
                ? MediaType.APPLICATION_JSON
                : MediaType.APPLICATION_XML;

        resultActions = mockMvc.perform(get(url).accept(mediaType));
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int statusCode) throws Exception {
        resultActions.andExpect(status().is(statusCode));
    }

    @And("the response should contain {int} users")
    public void theResponseShouldContainUsers(int count) throws Exception {
        resultActions.andExpect(jsonPath("$.length()").value(count));
    }

    @And("the response should contain a user with name {string} and email {string}")
    public void theResponseShouldContainUser(String name, String email) throws Exception {
        resultActions.andExpect(jsonPath("$[?(@.name == '" + name + "' && @.email == '" + email + "')]").exists());
    }
}
