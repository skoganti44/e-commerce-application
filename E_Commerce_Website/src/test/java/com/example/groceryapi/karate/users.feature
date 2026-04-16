Feature: Get Users API

  Background:
    * url 'http://localhost:8080'

  Scenario: Successfully fetch all users
    Given path '/users'
    And header Accept = 'application/json'
    When method get
    Then status 200
    And match response == '#array'
    And match each response contains { name: '#string', email: '#string' }

  Scenario: Fetch users returns a list
    Given path '/users'
    And header Accept = 'application/json'
    When method get
    Then status 200
    And match each response contains { name: '#string', email: '#string', password: '#string' }

  Scenario: Each user has all required fields
    Given path '/users'
    And header Accept = 'application/json'
    When method get
    Then status 200
    And match each response == { userid: '#number', name: '#string', email: '#string', password: '#string', createdat: '#string' }

  Scenario: Fetch users with wrong Accept header returns 406
    Given path '/users'
    And header Accept = 'application/xml'
    When method get
    Then status 406
