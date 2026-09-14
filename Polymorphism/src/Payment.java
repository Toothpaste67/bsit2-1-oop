public abstract class Payment {

    private final int id;
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

    public abstract void pay();
    public abstract String provider();

    public double serviceFee() {
        return amount * 0.02;
    }

    public double serviceFee(double rate) {
        return amount * rate;
    }

    public void printReceipt() {
        System.out.printf("[%d] %-6s %-10s PHP %10.2f%n", id, provider(), payerName, amount);
        pay();
    }

    public void printThankYou() {
        System.out.println("   Thank you for your payment.");
    }
}