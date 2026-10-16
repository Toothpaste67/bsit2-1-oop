package com.liceo.campustrack.model;

import com.liceo.campustrack.datastructure.ChainedHashSet;

/** A subject handled by one faculty member, with its set of enrolled students. */
public class CourseClass {
    private Long id;
    private String code;
    private String name;
    private Long facultyId;
    private String schedule;
    private final ChainedHashSet<Long> studentIds = new ChainedHashSet<>();

    public CourseClass() {}

    public boolean isEnrolled(Long studentId) { return studentIds.contains(studentId); }
    public String getLabel() { return code + " - " + name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public ChainedHashSet<Long> getStudentIds() { return studentIds; }
}
