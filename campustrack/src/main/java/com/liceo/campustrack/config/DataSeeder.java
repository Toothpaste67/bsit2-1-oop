package com.liceo.campustrack.config;

import com.liceo.campustrack.model.CourseClass;
import com.liceo.campustrack.model.Facility;
import com.liceo.campustrack.model.Role;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.repository.CourseClassRepository;
import com.liceo.campustrack.repository.FacilityRepository;
import com.liceo.campustrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * First-run data. Each block only runs while its .txt file is still empty,
 * so existing data is never overwritten.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final FacilityRepository facilities;
    private final CourseClassRepository classes;
    private final PasswordEncoder passwordEncoder;

    @Value("${campustrack.seed.admin-password}") private String adminPassword;
    @Value("${campustrack.seed.faculty-password}") private String facultyPassword;
    @Value("${campustrack.seed.student-password}") private String studentPassword;

    public DataSeeder(UserRepository users, FacilityRepository facilities,
                      CourseClassRepository classes, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.facilities = facilities;
        this.classes = classes;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (facilities.count() == 0) {
            facility("Audio Visual Room 1", "AVR-1", "3rd Floor, Rodelsa Hall", 150,
                    "Multimedia projection, air-conditioned, sound system");
            facility("Computer Laboratory 3", "COMP-LAB3", "ICT Building, 2nd Floor", 45,
                    "45 workstations with high-speed internet and IDE tools");
            facility("University Gymnasium", "GYM-MAIN", "Sports Complex", 500,
                    "Indoor basketball and volleyball court");
            facility("Executive Conference Room", "CONF-EXEC", "Administration Building", 25,
                    "Conference table, smart TV, teleconferencing equipment");
            facility("Library Study Area", "LIB-STUDY", "Main Library, 2nd Floor", 60,
                    "Quiet study tables with power outlets");
        }
        if (users.count() == 0) {
            user("System Administrator", "admin@liceo.edu.ph", adminPassword, Role.ADMIN, "ADM-2026-001");
            User faculty = user("Dr. Maria Santos", "hehe@liceo.edu.ph", facultyPassword, Role.FACULTY, "FAC-2026-102");
            User student = user("Juan Dela Cruz", "wanyu@liceo.edu.ph", studentPassword, Role.STUDENT, "2023-00123");

            if (classes.count() == 0) {
                CourseClass demo = new CourseClass();
                demo.setCode("IT 211");
                demo.setName("Data Structures and Algorithms");
                demo.setFacultyId(faculty.getId());
                demo.setSchedule("MWF 8:00-9:00 AM");
                demo.getStudentIds().add(student.getId());
                classes.save(demo);
            }
        }
    }

    private void facility(String name, String code, String location, int capacity, String description) {
        Facility facility = new Facility();
        facility.setName(name);
        facility.setCode(code);
        facility.setLocation(location);
        facility.setCapacity(capacity);
        facility.setDescription(description);
        facilities.save(facility);
    }

    private User user(String fullName, String email, String password, Role role, String idNumber) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setIdNumber(idNumber);
        return users.save(user);
    }
}
