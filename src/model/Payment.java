package model;

import java.time.LocalDateTime;

/**
 * ABSTRACTION: every payment method must implement processPayment() in its own way.
 * POLYMORPHISM: BookingService just calls payment.processPayment() and the right
 * subclass (UPIPayment or CardPayment) runs.
 *
 * NOTE: this project simulates payments (no real money moves). A real app would call
 * a gateway such as Razorpay inside processPayment().
 */
public abstract class Payment {

    private int paymentId;
    private int bookingId;
    private double amount;     // set by BookingService once the booking price is known
    private PaymentStatus status = PaymentStatus.PENDING;
    private LocalDateTime timestamp = LocalDateTime.now();

    protected Payment() { }

    /** Validates and "charges" the payment. Must set status to SUCCESS or FAILED. */
    public abstract boolean processPayment();

    /** 'UPI' or 'CARD' - stored in the payments table. */
    public abstract String getMethod();

    /** Safe reference to store: a UPI id, or only the last 4 card digits. */
    public abstract String getReference();

    public void refund() {
        if (status == PaymentStatus.SUCCESS) status = PaymentStatus.REFUNDED;
    }

    protected void markSuccess() { status = PaymentStatus.SUCCESS; timestamp = LocalDateTime.now(); }
    protected void markFailed()  { status = PaymentStatus.FAILED;  timestamp = LocalDateTime.now(); }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
