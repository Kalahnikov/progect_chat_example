package org.example;

public enum ROLE {
    USER("User1"),
    ADMIN("Admin1");

    private String role;

    ROLE(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return role;
    }
}
