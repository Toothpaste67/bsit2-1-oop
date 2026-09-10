// Main.java
// This file is COMPLETE. You do not need to change anything here.

import java.util.Scanner;

public class Main {

    private static Scanner input = new Scanner(System.in);
    private static UserManager manager = new UserManager();
    private static int nextId = 1;

    public static void main(String[] args) {
        seedSampleUsers();

        boolean running = true;

        while (running) {
            showMenu();

            String choice = input.nextLine().trim();

            if (choice.equals("1")) {
                addUser();
            } else if (choice.equals("2")) {
                manager.listAll();
            } else if (choice.equals("3")) {
                searchUser();
            } else if (choice.equals("4")) {
                deleteUser();
            } else if (choice.equals("5")) {
                manager.exportAll();
            } else if (choice.equals("6")) {
                System.out.println("Goodbye!");
                running = false;
            } else {
                System.out.println("Invalid choice. Please enter 1 to 6.");
            }

            System.out.println();
        }
    }

    private static void showMenu() {
        System.out.println("===== USER MANAGEMENT SYSTEM =====");
        System.out.println("1. Add user");
        System.out.println("2. List all users");
        System.out.println("3. Search user by ID");
        System.out.println("4. Delete user by ID");
        System.out.println("5. Export all users");
        System.out.println("6. Exit");
        System.out.print("Choose an option: ");
    }

    private static void addUser() {
        System.out.println("Type of user: 1 = Admin 2 = Teacher 3 = Student");
        System.out.print("Choose type: ");

        String type = input.nextLine().trim();

        System.out.print("Name: ");
        String name = input.nextLine().trim();

        System.out.print("Email: ");
        String email = input.nextLine().trim();

        User user;

        if (type.equals("1")) {
            user = new Admin(nextId, name, email);

        } else if (type.equals("2")) {
            System.out.print("Department: ");
            String dept = input.nextLine().trim();

            user = new Teacher(nextId, name, email, dept);

        } else if (type.equals("3")) {
            System.out.print("Course: ");
            String course = input.nextLine().trim();

            user = new Student(nextId, name, email, course);

        } else {
            System.out.println("Unknown type. User was not added.");
            return;
        }

        manager.add(user);
        nextId++;
    }

    private static void searchUser() {
        System.out.print("Enter ID to search: ");

        int id = readInt();

        User found = manager.findById(id);

        if (found == null) {
            System.out.println("No user found with ID " + id + ".");
        } else {
            System.out.println("Found:");
            found.display();
        }
    }

    private static void deleteUser() {
        System.out.print("Enter ID to delete: ");

        int id = readInt();

        if (manager.deleteById(id)) {
            System.out.println("User " + id + " was deleted.");
        } else {
            System.out.println("No user found with ID " + id + ".");
        }
    }

    private static int readInt() {
        while (true) {
            String line = input.nextLine().trim();

            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.print("That is not a number. Try again: ");
            }
        }
    }

    private static void seedSampleUsers() {
        manager.add(new Admin(nextId, "Razz", "razz@liceo.edu.ph"));
        nextId++;

        manager.add(new Teacher(nextId, "Maria", "maria@liceo.edu.ph", "CIT"));
        nextId++;

        manager.add(new Student(nextId, "Ana", "ana@liceo.edu.ph", "BSIT"));
        nextId++;

        System.out.println();
    }
}