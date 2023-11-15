package org.example;

public class User {

    private int id;
    private String login;
    private String password;
    private String username;
    private String role;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public User(int id, String login, String password, String username, String role) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.username = username;
        this.role = role;
    }

}
