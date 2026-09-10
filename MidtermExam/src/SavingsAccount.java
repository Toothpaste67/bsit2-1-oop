public class SavingsAccount extends Account {

    public static final double MAINTAINING_BALANCE = 500.0;

    private double interestRate;

    public SavingsAccount(String accountNumber, String ownerName,
                          double openingBalance, double interestRate) {
        super(accountNumber, ownerName, openingBalance);
        this.interestRate = interestRate;
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }

        double remaining = getBalance() - amount;
        if (remaining < MAINTAINING_BALANCE) {
            double shortfall = MAINTAINING_BALANCE - remaining;
            throw new InsufficientFundsException(shortfall);
        }
        super.withdraw(amount);
    }

    public double monthlyInterest() {
        return getBalance() * interestRate / 12;
    }
}
