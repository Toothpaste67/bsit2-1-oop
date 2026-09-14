import java.util.ArrayList;

public class PaymentGateway {

    private ArrayList<Payment> payments = new ArrayList<>();

    public void add(Payment payment) {
        payments.add(payment);
    }

    public int count() {
        return payments.size();
    }

    public void processAll() {
        if (payments.isEmpty()) {
            System.out.println("No payments have been made yet.");
            return;
        }
        for (Payment p : payments) {
            p.printReceipt();
            p.printThankYou();
        }
    }

    public void showServiceFees() {
        if (payments.isEmpty()) {
            System.out.println("No payments have been made yet.");
            return;
        }
        for (Payment p : payments) {
            System.out.printf("[%d] %-6s standard 2%%: PHP %8.2f student 1%%: PHP %8.2f%n",
                    p.getId(), p.provider(), p.serviceFee(), p.serviceFee(0.01));
        }
    }

    public Payment findById(int id) {
        for (Payment p : payments) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    public double totalCollected() {
        double total = 0;
        for (Payment p : payments) {
            total += p.getAmount();
        }
        return total;
    }

    public void refundAll() {
        int found = 0;
        for (Payment p : payments) {
            if (p instanceof Refundable) {
                Refundable r = (Refundable) p;
                r.printRefundNotice();
                found++;
            }
        }
        if (found == 0) {
            System.out.println("No refundable payments were found.");
        }
    }
}