package service;

import dao.BookingDAO;
import dao.PaymentDAO;
import dao.TimeSlotDAO;
import dao.TurfDAO;
import db.DBConnection;
import db.DataAccessException;
import model.Booking;
import model.BookingStatus;
import model.Payment;
import model.PaymentStatus;
import model.Player;
import model.TimeSlot;
import model.Turf;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The booking rules: search, book (with payment), cancel, history.
 * UI code calls only this class; it never touches SQL.
 */
public class BookingService {

    private final TurfDAO turfDAO = new TurfDAO();
    private final TimeSlotDAO slotDAO = new TimeSlotDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    public List<Turf> searchTurfs(String sport, String location) {
        return turfDAO.search(sport, location);
    }

    public List<TimeSlot> getAvailableSlots(int turfId) {
        return slotDAO.findByTurf(turfId, true);
    }

    /**
     * Books a slot and pays for it. Everything happens in ONE database transaction:
     * either the whole booking succeeds, or nothing is changed.
     *
     * @param payment a new UPIPayment or CardPayment (amount is filled in here)
     * @return the CONFIRMED booking
     * @throws IllegalStateException   slot taken / turf not approved / slot in the past
     * @throws PaymentFailedException  payment details were rejected (slot is freed again)
     */
    public Booking bookSlot(Player player, int slotId, Payment payment) {
        if (player == null || payment == null) throw new IllegalArgumentException("Player and payment are required");

        Booking booking = null;
        boolean paid = false;

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TimeSlot slot = slotDAO.findById(c, slotId)
                        .orElseThrow(() -> new IllegalArgumentException("That slot does not exist"));
                Turf turf = turfDAO.findById(slot.getTurfId())
                        .orElseThrow(() -> new IllegalArgumentException("That turf does not exist"));

                if (!turf.isApproved()) throw new IllegalStateException("This turf is not open for booking yet");
                if (slot.isInPast()) throw new IllegalStateException("This slot has already started or passed");

                // Atomic reservation: returns false if someone else got there first.
                if (!slotDAO.markBooked(c, slotId)) {
                    throw new IllegalStateException("Sorry, this slot was just booked by someone else");
                }
                slot.book();   // keep the in-memory object in sync with the database

                booking = new Booking(player.getUserId(), turf, slot);   // status PENDING, amount calculated
                bookingDAO.insert(c, booking);

                payment.setAmount(booking.getTotalAmount());
                payment.setBookingId(booking.getBookingId());
                booking.setPayment(payment);

                // POLYMORPHISM: runs UPIPayment's or CardPayment's own version.
                paid = payment.processPayment();
                paymentDAO.insert(c, payment);

                if (paid) {
                    bookingDAO.transition(c, booking.getBookingId(), BookingStatus.PENDING, BookingStatus.CONFIRMED);
                    booking.confirm();
                } else {
                    bookingDAO.transition(c, booking.getBookingId(), BookingStatus.PENDING, BookingStatus.CANCELLED);
                    booking.cancel();
                    slotDAO.release(c, slotId);
                    slot.release();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Booking failed because of a database error", e);
        }

        if (!paid) {
            throw new PaymentFailedException("Payment failed. Please check your payment details and try again.");
        }
        return booking;
    }

    /** Cancels a CONFIRMED booking before the slot starts, frees the slot and marks the payment REFUNDED. */
    public void cancelBooking(Player player, int bookingId) {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Booking b = bookingDAO.findById(c, bookingId)
                        .filter(x -> x.getPlayerId() == player.getUserId())   // can only cancel your own
                        .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

                if (b.getStatus() != BookingStatus.CONFIRMED) {
                    throw new IllegalStateException("Only confirmed bookings can be cancelled");
                }
                if (b.getSlot().isInPast()) {
                    throw new IllegalStateException("You cannot cancel a slot that has already started");
                }
                if (!bookingDAO.transition(c, bookingId, BookingStatus.CONFIRMED, BookingStatus.CANCELLED)) {
                    throw new IllegalStateException("This booking was already cancelled");
                }
                slotDAO.release(c, b.getSlot().getSlotId());
                paymentDAO.updateStatusByBooking(c, bookingId, PaymentStatus.REFUNDED);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Cancellation failed because of a database error", e);
        }
    }

    /** Loads the player's bookings (newest first) and also stores them inside the Player object. */
    public List<Booking> getBookingHistory(Player player) {
        List<Booking> list = bookingDAO.findByPlayer(player.getUserId());
        player.setBookingHistory(list);
        return list;
    }

    /** Returns all distinct turfs for which this player has at least one confirmed booking. */
    public List<Turf> getBookedTurfs(Player player) {
        if (player == null) return new ArrayList<>();
        List<Booking> list = bookingDAO.findByPlayer(player.getUserId());
        Map<Integer, Turf> map = new LinkedHashMap<>();
        for (Booking b : list) {
            if (b.getStatus() == BookingStatus.CONFIRMED && b.getTurf() != null) {
                map.putIfAbsent(b.getTurf().getTurfId(), b.getTurf());
            }
        }
        return new ArrayList<>(map.values());
    }
}
