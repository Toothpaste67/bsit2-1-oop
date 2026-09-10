public class Account {
    private String owner;
    private double balance;

    public Account(String owner, double openingBalance) {
        this.owner = owner;
        if (openingBalance < 0) {
            openingBalance = 0;
        }
        this.balance = openingBalance;
    }

    public String getOwner() {
        return owner;
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            System.out.println("Invalid amount.");
            return;
        }
        balance = balance + amount;
        System.out.println("Deposited " + amount + ". New balance: " + balance);
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("Invalid amount.");
            return;
        }
        if (amount > balance) {
            System.out.println("Insufficient funds.");
            return;
        }
        balance = balance - amount;
        System.out.println("Withdrew " + amount + ". New balance: " + balance);
    }
}