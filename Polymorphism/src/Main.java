import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PaymentGateway gateway = new PaymentGateway();
        int nextId = 1001;

        gateway.add(new GCashPayment(nextId++, "Ana", 1500.00, "0917-555-0134"));
        gateway.add(new MayaPayment(nextId++, "Jerome", 899.50, "jerome@liceo.edu.ph"));
        gateway.add(new CashPayment(nextId++, "Liza", 250.00));

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("===== LICEO PAY =====");
            System.out.println("1. Make a payment");
            System.out.println("2. Show all receipts");
            System.out.println("3. Find payment by ID");
            System.out.println("4. Show total collected");
            System.out.println("5. Refund all refundable payments");
            System.out.println("6. Show service fees");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Provider (1=GCash, 2=Maya, 3=Cash): ");
                    String provider = sc.nextLine().trim();
                    System.out.print("Payer name: ");
                    String name = sc.nextLine().trim();
                    System.out.print("Amount: ");
                    double amount = Double.parseDouble(sc.nextLine().trim());

                    if (provider.equals("1")) {
                        System.out.print("Mobile number: ");
                        String mobile = sc.nextLine().trim();
                        GCashPayment p = new GCashPayment(nextId++, name, amount, mobile);
                        gateway.add(p);
                        p.printReceipt();
                        p.printThankYou();
                    } else if (provider.equals("2")) {
                        System.out.print("Email: ");
                        String email = sc.nextLine().trim();
                        MayaPayment p = new MayaPayment(nextId++, name, amount, email);
                        gateway.add(p);
                        p.printReceipt();
                        p.printThankYou();
                    } else if (provider.equals("3")) {
                        CashPayment p = new CashPayment(nextId++, name, amount);
                        gateway.add(p);
                        p.printReceipt();
                        p.printThankYou();
                    } else {
                        System.out.println("Invalid provider.");
                    }
                    break;

                case "2":
                    gateway.processAll();
                    break;

                case "3":
                    System.out.print("Enter ID: ");
                    try {
                        int id = Integer.parseInt(sc.nextLine().trim());
                        Payment found = gateway.findById(id);
                        if (found != null) {
                            found.printReceipt();
                        } else {
                            System.out.println("No payment found with ID " + id);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid number.");
                    }
                    break;

                case "4":
                    System.out.printf("Total collected: PHP %.2f%n", gateway.totalCollected());
                    break;

                case "5":
                    System.out.println("Refunding every payment that can be refunded:");
                    gateway.refundAll();
                    break;

                case "6":
                    System.out.println("Service fees (the two serviceFee methods):");
                    gateway.showServiceFees();
                    break;

                case "0":
                    running = false;
                    System.out.println("Goodbye!");
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }

        sc.close();
    }
}