package model;

import java.time.LocalDateTime;

/** Connects a Player, a Turf, a TimeSlot and one Payment. */
public class Booking {

    private int bookingId;
    private int playerId;
    private String playerName;               // loaded from users table via JOIN
    private Turf turf;
    private TimeSlot slot;
    private Payment payment;                 // set after payment is made (not loaded from the DB)
    private double totalAmount;
    private BookingStatus status = BookingStatus.PENDING;
    private LocalDateTime bookingDate = LocalDateTime.now();

    public Booking(int bookingId, int playerId, Turf turf, TimeSlot slot,
                   double totalAmount, BookingStatus status, LocalDateTime bookingDate) {
        this.bookingId = bookingId;
        this.playerId = playerId;
        this.turf = turf;
        this.slot = slot;
        this.totalAmount = totalAmount;
        this.status = status;
        this.bookingDate = bookingDate;
    }

    /** New, unsaved booking. */
    public Booking(int playerId, Turf turf, TimeSlot slot) {
        this.playerId = playerId;
        this.turf = turf;
        this.slot = slot;
        this.totalAmount = calculateAmount();
    }

    /** price per hour x slot length in hours. */
    public double calculateAmount() {
        return Math.round(turf.getPricePerHour() * slot.getDurationHours() * 100.0) / 100.0;
    }

    public void confirm() { status = BookingStatus.CONFIRMED; }
    public void cancel()  { status = BookingStatus.CANCELLED; }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public int getPlayerId() { return playerId; }
    public String getPlayerName() {
        return (playerName != null && !playerName.isBlank()) ? playerName : ("Player #" + playerId);
    }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Turf getTurf() { return turf; }
    public TimeSlot getSlot() { return slot; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public double getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getBookingDate() { return bookingDate; }

    public String getPaymentMethodDisplay() {
        if (payment == null) return "N/A";
        String method = payment.getMethod();
        String ref = payment.getReference();
        if (ref != null && !ref.isBlank()) {
            return method + " (" + ref + ")";
        }
        return method != null ? method : "N/A";
    }

    @Override
    public String toString() {
        return "#" + bookingId + " " + turf.getName() + " | " + slot + " | Rs " + totalAmount + " | " + status;
    }
}
