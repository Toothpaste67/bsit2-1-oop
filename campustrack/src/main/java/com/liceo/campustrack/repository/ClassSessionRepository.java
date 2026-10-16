package com.liceo.campustrack.repository;

import com.liceo.campustrack.model.ClassSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class ClassSessionRepository extends TextFileRepository<ClassSession> {

    public ClassSessionRepository(@Value("${campustrack.data-dir}") String dataDir) {
        super(dataDir, "sessions.txt", "id|classId|facultyId|secret|startedAt|endedAt");
    }

    @Override protected Long getId(ClassSession s) { return s.getId(); }
    @Override protected void setId(ClassSession s, Long id) { s.setId(id); }

    @Override
    protected String[] toFields(ClassSession s) {
        return new String[] {
                text(s.getId()), text(s.getClassId()), text(s.getFacultyId()),
                text(s.getSecret()), text(s.getStartedAt()), text(s.getEndedAt())
        };
    }

    @Override
    protected ClassSession fromFields(String[] f) {
        ClassSession s = new ClassSession();
        s.setId(number(f, 0));
        s.setClassId(number(f, 1));
        s.setFacultyId(number(f, 2));
        s.setSecret(field(f, 3));
        s.setStartedAt(dateTime(f, 4));
        s.setEndedAt(dateTime(f, 5));
        return s;
    }
}
