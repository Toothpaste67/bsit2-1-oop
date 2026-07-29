import java.util.*;

public class Main {
    static double[] cutoffs = {90, 80, 70, 60};
    static char[] letters = {'A', 'B', 'C', 'D'};

    static char letterFor(double grade) {
        for (int i = 0; i < cutoffs.length; i++) {
            if (grade >= cutoffs[i]) return letters[i];
        }
        return 'F';
    }

    public static void main(String[] args) {
        ArrayList<Student> roster = new ArrayList<>();
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n===== GRADE TRACKER =====");
            System.out.println("1. Add student");
            System.out.println("2. View all students");
            System.out.println("3. Class average");
            System.out.println("4. Exit");
            System.out.print("Choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {
                System.out.print("Name: ");
                String name = sc.next();
                System.out.print("Grade (0-100): ");
                double grade = sc.nextDouble();
                roster.add(new Student(name, grade));
                System.out.println("Added " + name + "!");
            } else if (choice == 2) {
                if (roster.isEmpty()) {
                    System.out.println("No students yet.");
                } else {
                    for (Student s : roster) {
                        System.out.println(s.name + " - " + s.grade + " (" + letterFor(s.grade) + ")");
                    }
                }
            } else if (choice == 3) {
                if (roster.isEmpty()) {
                    System.out.println("No students yet.");
                } else {
                    double total = 0;
                    for (Student s : roster) {
                        total += s.grade;
                    }
                    double average = total / roster.size();
                    System.out.printf("Class average: %.2f (%s)%n", average, letterFor(average));
                }
            } else if (choice == 4) {
                running = false;
                System.out.println("Goodbye!");
            } else {
                System.out.println("Invalid choice. Pick 1-4.");
            }
        }
        sc.close();
    }
}

class Student {
    String name;
    double grade;

    Student(String name, double grade) {
        this.name = name;
        this.grade = grade;
    }
}

//gwapo ko