public class MayaPayment extends Payment implements Refundable {

    private String email;

    public MayaPayment(int id, String payerName, double amount, String email) {
        super(id, payerName, amount);
        this.email = email;
    }

    public String getEmail() { return email; }

    @Override
    public String provider() {
        return "MAYA";
    }

    @Override
    public void pay() {
        System.out.printf("   Maya: PHP %.2f charged to the wallet of %s.%n", getAmount(), email);
    }

    @Override
    public String refund() {
        return "Maya refund of PHP " + String.format("%.2f", getAmount()) + " emailed to " + email + ".";
    }
}