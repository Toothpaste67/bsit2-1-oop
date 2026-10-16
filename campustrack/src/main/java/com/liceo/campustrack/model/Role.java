package com.liceo.campustrack.model;

public enum Role {
    ADMIN("Admin"),
    FACULTY("Faculty"),
    STUDENT("Student");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }
}
