package service;

/** Thrown when the payment step of a booking fails. The booking is saved as CANCELLED and the slot is freed. */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}
