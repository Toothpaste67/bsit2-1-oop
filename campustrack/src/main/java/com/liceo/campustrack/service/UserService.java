package com.liceo.campustrack.service;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.datastructure.Sorting;
import com.liceo.campustrack.model.CourseClass;
import com.liceo.campustrack.model.Role;
import com.liceo.campustrack.model.User;
import com.liceo.campustrack.repository.CourseClassRepository;
import com.liceo.campustrack.repository.UserRepository;
import com.liceo.campustrack.security.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class UserService implements UserDetailsService {

    private static final String EMAIL_DOMAIN = "@liceo.edu.ph";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final CourseClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(UserRepository userRepository, CourseClassRepository classRepository,
                       PasswordEncoder passwordEncoder, AuditService audit) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) throw new UsernameNotFoundException("User not found with email: " + email);
        return new CustomUserDetails(user);
    }

    /** Self-registration always creates a STUDENT. Faculty and admin accounts are made by an admin. */
    public synchronized User register(String fullName, String email, String idNumber, String password) {
        User user = apply(new User(), fullName, email, idNumber, password, Role.STUDENT);
        audit.log(user, "REGISTER", "Student account created");
        return user;
    }

    /** Create (id == null) or edit a user from the admin page. A blank password keeps the old one. */
    public synchronized User saveByAdmin(User admin, Long id, String fullName, String email, String idNumber,
                                         String password, Role role) {
        User target = new User();
        if (id != null) {
            target = userRepository.findById(id);
            if (target == null) throw new IllegalArgumentException("User not found");
            if (target.getId().equals(admin.getId()) && role != Role.ADMIN) {
                throw new IllegalArgumentException("You cannot remove your own admin role");
            }
        }
        User saved = apply(target, fullName, email, idNumber, password, role);
        audit.log(admin, id == null ? "USER_CREATE" : "USER_UPDATE",
                saved.getEmail() + " (" + saved.getRole() + ")");
        return saved;
    }

    public synchronized void delete(User admin, Long id) {
        User target = userRepository.findById(id);
        if (target == null) throw new IllegalArgumentException("User not found");
        if (target.getId().equals(admin.getId())) throw new IllegalArgumentException("You cannot delete your own account");

        for (CourseClass courseClass : classRepository.findAll()) {
            if (courseClass.getStudentIds().remove(id)) classRepository.save(courseClass);
        }
        userRepository.deleteById(id);
        audit.log(admin, "USER_DELETE", target.getEmail());
    }

    /** Filtered user list, sorted by name with merge sort. */
    public DynamicArray<User> search(Role role, String keyword) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase();
        DynamicArray<User> matches = userRepository.findWhere(u ->
                (role == null || u.getRole() == role)
                        && (needle.isEmpty()
                        || u.getFullName().toLowerCase().contains(needle)
                        || u.getEmail().toLowerCase().contains(needle)
                        || u.getIdNumber().toLowerCase().contains(needle)));
        return Sorting.mergeSort(matches, Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER));
    }

    public User findById(Long id) { return userRepository.findById(id); }
    public int count() { return userRepository.count(); }
    public int count(Role role) { return userRepository.findWhere(u -> u.getRole() == role).size(); }

    private User apply(User user, String fullName, String email, String idNumber, String password, Role role) {
        fullName = clean(fullName);
        email = clean(email).toLowerCase();
        idNumber = clean(idNumber);

        require(!fullName.isEmpty() && fullName.length() <= 100, "Full name is required (up to 100 characters)");
        require(email.endsWith(EMAIL_DOMAIN) && email.length() > EMAIL_DOMAIN.length() && email.length() <= 100,
                "Use your Liceo de Cagayan University email (" + EMAIL_DOMAIN + ")");
        require(!idNumber.isEmpty() && idNumber.length() <= 50, "ID number is required");
        require(role != null, "Role is required");

        User sameEmail = userRepository.findByEmail(email);
        require(sameEmail == null || sameEmail.getId().equals(user.getId()), "Email address is already registered");
        User sameIdNumber = userRepository.findByIdNumber(idNumber);
        require(sameIdNumber == null || sameIdNumber.getId().equals(user.getId()), "ID number is already registered");

        boolean passwordGiven = password != null && !password.isEmpty();
        require(passwordGiven || user.getId() != null, "Password is required");
        if (passwordGiven) {
            require(password.length() >= MIN_PASSWORD_LENGTH && password.length() <= 72,
                    "Password must be " + MIN_PASSWORD_LENGTH + " to 72 characters long");
            user.setPassword(passwordEncoder.encode(password));
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setIdNumber(idNumber);
        user.setRole(role);
        return userRepository.save(user);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
