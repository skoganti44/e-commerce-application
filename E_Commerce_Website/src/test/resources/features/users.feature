Feature: Get Users API
  As a client of the Grocery API
  I want to fetch all users
  So that I can view user information

  Scenario: Successfully fetch all users
    Given the following users exist in the system
      | name  | email             | password |
      | John  | john@example.com  | pass123  |
      | Jane  | jane@example.com  | pass456  |
    When I send a GET request to "/users" with Accept header "application/json"
    Then the response status code should be 200
    And the response should contain 2 users
    And the response should contain a user with name "John" and email "john@example.com"
    And the response should contain a user with name "Jane" and email "jane@example.com"

  Scenario: Fetch users when no users exist
    Given no users exist in the system
    When I send a GET request to "/users" with Accept header "application/json"
    Then the response status code should be 200
    And the response should contain 0 users

  Scenario: Fetch users with wrong Accept header
    When I send a GET request to "/users" with Accept header "application/xml"
    Then the response status code should be 406
