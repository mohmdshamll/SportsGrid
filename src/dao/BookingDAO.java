package dao;

import db.DBConnection;
import db.DataAccessException;
import model.Booking;
import model.BookingStatus;
import model.CardPayment;
import model.Payment;
import model.TimeSlot;
import model.Turf;
import model.UPIPayment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookingDAO {

    // one JOIN query gives us the booking together with its turf, slot, player, and payment
    private static final String SELECT =
            "SELECT b.booking_id, b.player_id, u.name AS player_name, b.total_amount, b.status, b.booking_date, "
          + "t.turf_id, t.owner_id, t.name AS turf_name, t.location, t.sport_type, t.price_per_hour, t.is_approved, "
          + "s.slot_id, s.slot_date, s.start_time, s.end_time, s.is_booked, "
          + "p.payment_id, p.method AS payment_method, p.reference AS payment_reference, p.status AS payment_status "
          + "FROM bookings b "
          + "JOIN users u ON u.user_id = b.player_id "
          + "JOIN turfs t ON t.turf_id = b.turf_id "
          + "JOIN time_slots s ON s.slot_id = b.slot_id "
          + "LEFT JOIN payments p ON p.booking_id = b.booking_id ";
    private static final String ORDER = " ORDER BY b.booking_date DESC, b.booking_id DESC";

    /** Inserts inside the caller's transaction. */
    public int insert(Connection c, Booking b) throws SQLException {
        String sql = "INSERT INTO bookings (player_id, turf_id, slot_id, total_amount, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getPlayerId());
            ps.setInt(2, b.getTurf().getTurfId());
            ps.setInt(3, b.getSlot().getSlotId());
            ps.setDouble(4, b.getTotalAmount());
            ps.setString(5, b.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                b.setBookingId(keys.getInt(1));
                return b.getBookingId();
            }
        }
    }

    /**
     * Changes status only if the booking is currently in the 'from' state.
     * Returns false if it was not (e.g. cancelled twice), so the caller can stop.
     */
    public boolean transition(Connection c, int bookingId, BookingStatus from, BookingStatus to) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE bookings SET status = ? WHERE booking_id = ? AND status = ?")) {
            ps.setString(1, to.name());
            ps.setInt(2, bookingId);
            ps.setString(3, from.name());
            return ps.executeUpdate() == 1;
        }
    }

    public Optional<Booking> findById(Connection c, int bookingId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE b.booking_id = ?")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Booking> findByPlayer(int playerId) { return query("WHERE b.player_id = ?", playerId); }

    /** All bookings made on turfs that this owner owns. */
    public List<Booking> findByOwner(int ownerId) { return query("WHERE t.owner_id = ?", ownerId); }

    /** All bookings across the platform for Admin. */
    public List<Booking> findAll() {
        List<Booking> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + ORDER);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load bookings", e);
        }
    }

    /** Sum of CONFIRMED booking amounts across the entire platform. */
    public double sumAllConfirmed() {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM bookings WHERE status = 'CONFIRMED'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not calculate total revenue", e);
        }
    }

    /** True if the player has a CONFIRMED booking at this turf (needed before they can review it). */
    public boolean hasConfirmedBooking(int playerId, int turfId) {
        String sql = "SELECT 1 FROM bookings WHERE player_id = ? AND turf_id = ? AND status = 'CONFIRMED' LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setInt(2, turfId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check bookings", e);
        }
    }

    /** Sum of CONFIRMED booking amounts across all turfs of this owner. */
    public double sumConfirmedForOwner(int ownerId) {
        String sql = "SELECT COALESCE(SUM(b.total_amount), 0) FROM bookings b "
                   + "JOIN turfs t ON t.turf_id = b.turf_id WHERE t.owner_id = ? AND b.status = 'CONFIRMED'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not calculate earnings", e);
        }
    }

    private List<Booking> query(String where, Object param) {
        List<Booking> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + where + ORDER)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load bookings", e);
        }
    }

    private Booking map(ResultSet rs) throws SQLException {
        Turf turf = new Turf(rs.getInt("turf_id"), rs.getInt("owner_id"), rs.getString("turf_name"),
                rs.getString("location"), rs.getString("sport_type"),
                rs.getDouble("price_per_hour"), rs.getBoolean("is_approved"));
        TimeSlot slot = new TimeSlot(rs.getInt("slot_id"), rs.getInt("turf_id"),
                rs.getDate("slot_date").toLocalDate(), rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(), rs.getBoolean("is_booked"));
        Booking booking = new Booking(rs.getInt("booking_id"), rs.getInt("player_id"), turf, slot,
                rs.getDouble("total_amount"), BookingStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("booking_date").toLocalDateTime());
        booking.setPlayerName(rs.getString("player_name"));

        String method = rs.getString("payment_method");
        if (method != null) {
            String ref = rs.getString("payment_reference");
            Payment payment;
            if ("CARD".equalsIgnoreCase(method)) {
                payment = new CardPayment("Cardholder", ref != null ? ref : "1111", "12/30");
            } else {
                payment = new UPIPayment(ref != null ? ref : "user@upi");
            }
            payment.setAmount(rs.getDouble("total_amount"));
            payment.setBookingId(rs.getInt("booking_id"));
            int pId = rs.getInt("payment_id");
            if (pId > 0) payment.setPaymentId(pId);
            String pStatus = rs.getString("payment_status");
            if (pStatus != null) {
                try {
                    payment.setStatus(model.PaymentStatus.valueOf(pStatus));
                } catch (IllegalArgumentException ignored) {}
            }
            booking.setPayment(payment);
        }
        return booking;
    }
}
