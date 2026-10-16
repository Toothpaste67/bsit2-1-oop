package com.liceo.campustrack.service;

import com.liceo.campustrack.model.CourseClass;
import com.liceo.campustrack.model.Facility;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.repository.CourseClassRepository;
import com.liceo.campustrack.repository.FacilityRepository;
import com.liceo.campustrack.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Records store ids, not objects. Templates use this bean to turn an id into a name,
 * e.g. {@code ${@names.user(record.userId)}}. Each call is one hash table lookup.
 */
@Component("names")
public class NameLookup {

    private final UserRepository users;
    private final CourseClassRepository classes;
    private final FacilityRepository facilities;

    public NameLookup(UserRepository users, CourseClassRepository classes, FacilityRepository facilities) {
        this.users = users;
        this.classes = classes;
        this.facilities = facilities;
    }

    public String user(Long id) {
        User user = users.findById(id);
        return user == null ? "(deleted user)" : user.getFullName();
    }

    public String idNumber(Long id) {
        User user = users.findById(id);
        return user == null ? "-" : user.getIdNumber();
    }

    public String role(Long id) {
        User user = users.findById(id);
        return user == null ? "-" : user.getRole().getLabel();
    }

    public String courseClass(Long id) {
        CourseClass courseClass = classes.findById(id);
        return courseClass == null ? "(deleted class)" : courseClass.getLabel();
    }

    public String facility(Long id) {
        Facility facility = facilities.findById(id);
        return facility == null ? "(removed facility)" : facility.getName();
    }
}
