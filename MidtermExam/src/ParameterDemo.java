public class ParameterDemo {

    public static void changeBalance(Account account) {

        account.deposit(1000);

        System.out.println(
                "Inside method: PHP "
                        + String.format("%.2f", account.getBalance())
        );
    }

    public static void reassignAccount(Account account) {

        account = null;

        System.out.println(
                "Inside method: account was reassigned to null."
        );
    }
}