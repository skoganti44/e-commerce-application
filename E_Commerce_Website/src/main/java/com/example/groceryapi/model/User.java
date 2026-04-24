//PLAIN OLD JAVA OBJECT(This is a JPA Entity- a java class that maps directly to a database table)
package com.example.groceryapi.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // Marks this class as a database table
@Table(name = "\"User\"", schema = "public")
public class User {

    @Id // Marks the primary key field
        // @GeneratedValue==>Auto-generates primary key values
        // GenerationType==>Defines HOW the key is generated
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userid;

    private String name;

    private String email;

    private String password;

    private LocalDateTime createdat;

    public int getuserid() {
        return userid;
    }

    public void setuserid(int userid) {
        this.userid = userid;
    }

    public String getname() {
        return name;
    }

    public void setname(String name) {
        this.name = name;
    }

    public String getemail() {
        return email;
    }

    public void setemail(String email) {
        this.email = email;
    }

    public String getpassword() {
        return password;
    }

    public void setpassword(String password) {
        this.password = password;
    }

    public LocalDateTime getcreatedat() {
        return createdat;
    }

    public void setcreatedat(LocalDateTime createdat) {
        this.createdat = createdat;
    }
}
