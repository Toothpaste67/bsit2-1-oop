public class CheckingAccount extends Account {

    public CheckingAccount(
            String accountNumber,
            String ownerName,
            double openingBalance) {

        super(accountNumber, ownerName, openingBalance);
    }

    @Override
    public String getAccountType() {
        return "CHECKING";
    }
}