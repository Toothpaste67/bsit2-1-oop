import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int[] id = new int[10];
        String[] name = new String[10];
        int[] age = new int[10];
        String[] course = new String[10];
        double[] grade = new double[10];
        boolean[] enrolled = new boolean[10];
        int count = 0;

        int choice;
        do {
            System.out.println("\n--- Student Information System ---");
            System.out.println("[1] Add Student");
            System.out.println("[2] View All Students");
            System.out.println("[3] Search Student");
            System.out.println("[4] View Statistics");
            System.out.println("[5] Exit");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();

            switch (choice) {
                case 1:
                    if (count >= 10) {
                        System.out.println("Student list is full.");
                        break;
                    }

                    System.out.print("ID: ");
                    id[count] = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Full Name: ");
                    name[count] = sc.nextLine();

                    System.out.print("Age: ");
                    int newAge = sc.nextInt();
                    if (newAge <= 0) {
                        System.out.println("[Invalid age. Must be positive. Student not added]");
                        break;
                    }
                    age[count] = newAge;
                    sc.nextLine();

                    System.out.print("Course: ");
                    course[count] = sc.nextLine();

                    System.out.print("Grade: ");
                    double newGrade = sc.nextDouble();
                    if (newGrade < 0 || newGrade > 100) {
                        System.out.println("[Invalid grade. Must be 0-100. Student not added]");
                        break;
                    }
                    grade[count] = newGrade;

                    System.out.print("Enrolled (true/false): ");
                    enrolled[count] = sc.nextBoolean();

                    count++;
                    System.out.println("[Student added]");
                    break;

                case 2:
                    if (count == 0) {
                        System.out.println("No students yet.");
                        break;
                    }

                    System.out.printf("%-5s %-15s %-5s %-10s %-8s %-10s %-15s%n",
                            "[ID]", "[Name]", "[Age]", "[Course]", "[Grade]", "[Enrolled]", "[Standing]");

                    for (int i = 0; i < count; i++) {
                        String standing;
                        if (grade[i] >= 90) {
                            standing = "Dean's Lister";
                        } else if (grade[i] >= 75) {
                            standing = "Passed";
                        } else {
                            standing = "Failed";
                        }

                        System.out.printf("%-5d %-15s %-5d %-10s %-8.2f %-10b %-15s%n",
                                id[i], name[i], age[i], course[i], grade[i], enrolled[i], standing);
                    }
                    break;

                case 3:
                    System.out.print("Enter ID to search: ");
                    int searchId = sc.nextInt();
                    boolean found = false;

                    for (int i = 0; i < count; i++) {
                        if (id[i] == searchId) {
                            System.out.println(id[i] + " | " + name[i] + " | " + age[i]
                                    + " | " + course[i] + " | " + grade[i] + " | " + enrolled[i]);
                            found = true;
                            break;
                        }
                    }

                    if (!found) System.out.println("[Student not found]");
                    break;

                case 4:
                    if (count == 0) {
                        System.out.println("[No students yet]");
                        break;
                    }

                    double sum = 0;
                    double topGrade = grade[0];
                    String topName = name[0];

                    for (int i = 0; i < count; i++) {
                        sum += grade[i];
                        if (grade[i] > topGrade) {
                            topGrade = grade[i];
                            topName = name[i];
                        }
                    }

                    System.out.println("Total Students: " + count);
                    System.out.println("Average Grade: " + (sum / count));
                    System.out.println("Top Student: " + topName + " (" + topGrade + ")");
                    break;

                case 5:
                    System.out.println("[Goodbye!]");
                    break;

                default:
                    System.out.println("[Invalid choice]");
            }
        } while (choice != 5);

        sc.close();
    }
}
