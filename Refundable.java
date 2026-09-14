// Refundable.java
// An INTERFACE = a list of promises. It says WHAT, never HOW.
//
// Only SOME payments can be refunded inside the app. GCash and Maya can.
// Cash cannot - that is refunded at the counter. So Refundable is a
// "can-do" contract, not a family relationship.

public interface Refundable {

    // TODO 1: Declare an abstract method called refund that returns a String
    // and takes no parameters.
    String refund();

    // TODO 2: Write a DEFAULT method called printRefundNotice that returns
    // nothing (void) and takes no parameters.
    // Inside it, print three spaces followed by the result of refund(),
    // like this: System.out.println("   " + refund());
    default void printRefundNotice() {
        System.out.println("   " + refund());
    }
}