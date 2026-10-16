package com.liceo.campustrack.repository;

import com.liceo.campustrack.datastructure.DynamicArray;
import com.liceo.campustrack.datastructure.HashTable;
import com.liceo.campustrack.model.Role;
import com.liceo.campustrack.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends TextFileRepository<User> {

    // Secondary hash tables: O(1) lookup by email (login) and by ID number (enrollment)
    private HashTable<String, User> byEmail = new HashTable<>();
    private HashTable<String, User> byIdNumber = new HashTable<>();

    public UserRepository(@Value("${campustrack.data-dir}") String dataDir) {
        super(dataDir, "users.txt", "id|fullName|email|passwordHash|role|idNumber|createdAt");
    }

    public synchronized User findByEmail(String email) {
        return email == null ? null : byEmail.get(email.trim().toLowerCase());
    }

    public synchronized User findByIdNumber(String idNumber) {
        return idNumber == null ? null : byIdNumber.get(idNumber.trim().toLowerCase());
    }

    @Override
    protected void reindex(DynamicArray<User> all) {
        HashTable<String, User> emails = new HashTable<>();
        HashTable<String, User> idNumbers = new HashTable<>();
        for (User user : all) {
            emails.put(user.getEmail().toLowerCase(), user);
            idNumbers.put(user.getIdNumber().toLowerCase(), user);
        }
        byEmail = emails;
        byIdNumber = idNumbers;
    }

    @Override protected Long getId(User user) { return user.getId(); }
    @Override protected void setId(User user, Long id) { user.setId(id); }

    @Override
    protected String[] toFields(User u) {
        return new String[] {
                text(u.getId()), text(u.getFullName()), text(u.getEmail()), text(u.getPassword()),
                text(u.getRole()), text(u.getIdNumber()), text(u.getCreatedAt())
        };
    }

    @Override
    protected User fromFields(String[] f) {
        User u = new User();
        u.setId(number(f, 0));
        u.setFullName(field(f, 1));
        u.setEmail(field(f, 2));
        u.setPassword(field(f, 3));
        u.setRole(constant(Role.class, f, 4));
        u.setIdNumber(field(f, 5));
        u.setCreatedAt(dateTime(f, 6));
        return u;
    }
}
