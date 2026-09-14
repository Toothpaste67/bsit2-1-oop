// Main.java
// COMPLETE - this is the menu that tests your work.
// It only ever talks to PaymentGateway and Payment - never to the
// GCashPayment / MayaPayment / CashPayment classes directly by name
// (except right here, where a NEW payment is actually created - that
// part has to happen somewhere, and it is not PaymentGateway's job).

import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final PaymentGateway gateway = new PaymentGateway();
    private static int nextId = 1004; // 1001-1003 are the preloaded samples

    public static void main(String[] args) {
        loadSamplePayments();
        printBanner();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    makePayment();
                    break;
                case "2":
                    System.out.println();
                    gateway.processAll();
                    break;
                case "3":
                    findPayment();
                    break;
                case "4":
                    System.out.println();
                    System.out.printf("Total collected so far: PHP %.2f%n", gateway.totalCollected());
                    System.out.println("Number of payments on record: " + gateway.count());
                    break;
                case "5":
                    System.out.println();
                    System.out.println("Refunding every payment that can be refunded:");
                    gateway.refundAll();
                    break;
                case "6":
                    System.out.println();
                    System.out.println("Service fees (the two serviceFee methods):");
                    gateway.showServiceFees();
                    break;
                case "0":
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Please choose a number from the menu.");
            }
            System.out.println();
        }

        scanner.close();
    }

    private static void loadSamplePayments() {
        gateway.add(new GCashPayment(1001, "Ana", 1500.00, "0917-555-0134"));
        gateway.add(new MayaPayment(1002, "Jerome", 899.50, "jerome@liceo.edu.ph"));
        gateway.add(new CashPayment(1003, "Liza", 250.00));
    }

    private static void printBanner() {
        System.out.println("=========================================");
        System.out.println("            LICEO PAY - v1.0            ");
        System.out.println("   Campus Canteen Payment Gateway CLI   ");
        System.out.println("=========================================");
    }

    private static void printMenu() {
        System.out.println("MAIN MENU");
        System.out.println("1. Make a new payment");
        System.out.println("2. Show all receipts");
        System.out.println("3. Find a payment by ID");
        System.out.println("4. Show total collected");
        System.out.println("5. Refund all refundable payments");
        System.out.println("6. Show service fees");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    private static void makePayment() {
        System.out.println();
        System.out.println("1) GCash   2) Maya   3) Cash");
        System.out.print("Choose a payment method: ");
        String method = scanner.nextLine().trim();

        System.out.print("Payer name: ");
        String name = scanner.nextLine().trim();

        double amount = readDouble("Amount: ");

        Payment payment;
        switch (method) {
            case "1":
                System.out.print("Mobile number: ");
                String mobile = scanner.nextLine().trim();
                payment = new GCashPayment(nextId++, name, amount, mobile);
                break;
            case "2":
                System.out.print("Email: ");
                String email = scanner.nextLine().trim();
                payment = new MayaPayment(nextId++, name, amount, email);
                break;
            case "3":
                payment = new CashPayment(nextId++, name, amount);
                break;
            default:
                System.out.println("Not a valid payment method. Payment cancelled.");
                return;
        }

        gateway.add(payment);
        System.out.println();
        payment.printReceipt();
        payment.printThankYou();
    }

    private static void findPayment() {
        System.out.print("Enter the ID to search for: ");
        String input = scanner.nextLine().trim();

        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("That is not a valid number.");
            return;
        }

        Payment found = gateway.findById(id);
        System.out.println();
        if (found == null) {
            System.out.println("No payment found with ID " + id + ".");
        } else {
            System.out.println("Payment found:");
            found.printReceipt();
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}