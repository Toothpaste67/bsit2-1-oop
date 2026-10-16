package com.liceo.campustrack.repository;

import com.liceo.campustrack.model.CourseClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class CourseClassRepository extends TextFileRepository<CourseClass> {

    public CourseClassRepository(@Value("${campustrack.data-dir}") String dataDir) {
        super(dataDir, "classes.txt", "id|code|name|facultyId|schedule|enrolledStudentIds");
    }

    @Override protected Long getId(CourseClass c) { return c.getId(); }
    @Override protected void setId(CourseClass c, Long id) { c.setId(id); }

    @Override
    protected String[] toFields(CourseClass c) {
        StringBuilder students = new StringBuilder();
        for (Long studentId : c.getStudentIds().values()) {
            if (students.length() > 0) students.append(',');
            students.append(studentId);
        }
        return new String[] {
                text(c.getId()), text(c.getCode()), text(c.getName()), text(c.getFacultyId()),
                text(c.getSchedule()), students.toString()
        };
    }

    @Override
    protected CourseClass fromFields(String[] f) {
        CourseClass c = new CourseClass();
        c.setId(number(f, 0));
        c.setCode(field(f, 1));
        c.setName(field(f, 2));
        c.setFacultyId(number(f, 3));
        c.setSchedule(field(f, 4));
        String students = field(f, 5);
        if (students != null) {
            for (String id : students.split(",")) {
                if (!id.isBlank()) c.getStudentIds().add(Long.valueOf(id.trim()));
            }
        }
        return c;
    }
}
