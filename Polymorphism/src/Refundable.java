public interface Refundable {
    String refund();

    default void printRefundNotice() {
        System.out.println("   " + refund());
    }
}