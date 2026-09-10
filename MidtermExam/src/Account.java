public abstract class Account {

    private String accountNumber;
    private String ownerName;
    private double balance;

    public Account(
            String accountNumber,
            String ownerName,
            double openingBalance) {

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Account number is required"
            );
        }

        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Owner name is required"
            );
        }

        if (openingBalance < 0) {
            throw new IllegalArgumentException(
                    "Opening balance cannot be negative"
            );
        }

        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = openingBalance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Deposit amount must be greater than 0"
            );
        }

        balance += amount;
    }

    public void withdraw(double amount)
            throws InsufficientFundsException {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount must be greater than 0"
            );
        }

        if (amount > balance) {
            throw new InsufficientFundsException(
                    amount - balance
            );
        }

        balance -= amount;
    }

    public abstract String getAccountType();

    protected void applyWithdrawal(double amount) {
        balance -= amount;
    }

    @Override
    public String toString() {
        return getAccountType()
                + " "
                + accountNumber
                + " ("
                + ownerName
                + ")";
    }
}