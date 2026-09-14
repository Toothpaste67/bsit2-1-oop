// CashPayment.java
// A CHILD of Payment that is NOT Refundable.
// Cash is refunded at the counter, not inside the app, so this class
// deliberately does NOT implement Refundable. Leaving it out is a design
// decision, not a mistake.

// TODO 9: Make this class extend Payment. Do NOT implement Refundable.
public class CashPayment extends Payment {

    public CashPayment(int id, String payerName, double amount) {
        super(id, payerName, amount);
    }

    @Override
    public String provider() {
        return "CASH";
    }

    @Override
    public void pay() {
        System.out.printf("  Cash: PHP %.2f received. Please get your change.%n", getAmount());
    }
}