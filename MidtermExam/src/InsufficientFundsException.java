public class InsufficientFundsException extends Exception {

    public InsufficientFundsException(double shortBy) {
        super(String.format(
                "Insufficient funds. You are short by PHP %.2f",
                shortBy
        ));
    }
}