// PaymentGateway.java
// This class NEVER writes the words GCashPayment, MayaPayment or CashPayment.
// It talks only to the abstraction, Payment. Keep it that way - it is graded.

import java.util.ArrayList;

public class PaymentGateway {

    private ArrayList<Payment> payments = new ArrayList<>();

    public void add(Payment payment) {
        payments.add(payment);
    }

    public int count() {
        return payments.size();
    }

    // Already written for you. One loop, one call, three different behaviours.
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

    // Already written for you. This calls BOTH of your serviceFee methods,
    // so it only works once TODO 5 is done.
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

    // TODO 10: Finish findById(int id).
    // Loop through payments. If a payment's getId() equals id,
    // return that payment. If the loop finishes and nothing
    // matched, return null.
    public Payment findById(int id) {
        for (Payment p : payments) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    // TODO 11: Finish totalCollected().
    // Start a double called total at 0. Loop through payments and
    // add each p.getAmount() to it. Return total.
    public double totalCollected() {
        double total = 0;
        for (Payment p : payments) {
            total += p.getAmount();
        }
        return total;
    }

    // TODO 12: Finish refundAll().
    // Loop through payments. For each one:
    // - ask if (p instanceof Refundable)
    // - if true, downcast: Refundable r = (Refundable) p;
    // - then call r.printRefundNotice();
    // - and add 1 to found
    // Cash payments are skipped automatically, because a
    // CashPayment is not Refundable. Never cast without asking first.
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