// Payment.java
// The ABSTRACTION. This class holds everything that is true of EVERY payment.
// It must never be created directly - nobody should be able to write
// new Payment(...) because "a payment" with no method is not a real thing.

// TODO 3: Add the keyword "abstract" before the word class.
public abstract class Payment {

    private final int id; // given to you - do not change
    private String payerName;
    private double amount;

    public Payment(int id, String payerName, double amount) {
        this.id = id;
        this.payerName = payerName;
        this.amount = amount;
    }

    public int getId() { return id; }
    public String getPayerName() { return payerName; }
    public double getAmount() { return amount; }

    // TODO 4: Declare TWO abstract methods here.
    // (a) pay() - returns nothing (void), takes no parameters
    // (b) provider() - returns a String, takes no parameters
    public abstract void pay();
    public abstract String provider();

    // This is version 1 of serviceFee. It is already written for you.
    // The standard fee is 2% of the amount.
    public double serviceFee() {
        return amount * 0.02;
    }

    // TODO 5: This is OVERLOADING. Write a SECOND method, also called
    // serviceFee, that takes one parameter: double rate.
    // It must return amount multiplied by rate.
    // Same name, different parameter list - that is all overloading is.
    public double serviceFee(double rate) {
        return amount * rate;
    }

    // TODO 6: Write ONE System.out.printf line inside printReceipt().
    // Use this exact format string:
    // "[%d] %-6s %-10s PHP %10.2f%n"
    // It needs four values, in this order:
    // id, provider(), payerName, amount
    // Notice this method calls provider() and pay(), which still
    // have no body here. That is allowed - and it is exactly what
    // polymorphism is for. The child decides what actually runs.
    public void printReceipt() {
        System.out.printf("[%d] %-6s %-10s PHP %10.2f%n", id, provider(), payerName, amount);
        pay();
    }

    // Already written for you. Most children will simply inherit this.
    public void printThankYou() {
        System.out.println("  Thank you for your payment.");
    }
}