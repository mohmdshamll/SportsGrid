package dao;

import model.Payment;
import model.PaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Payments are always written inside the booking transaction, so every method takes the Connection. */
public class PaymentDAO {

    public int insert(Connection c, Payment p) throws SQLException {
        String sql = "INSERT INTO payments (booking_id, method, reference, amount, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getBookingId());
            ps.setString(2, p.getMethod());
            ps.setString(3, p.getReference());
            ps.setDouble(4, p.getAmount());
            ps.setString(5, p.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                p.setPaymentId(keys.getInt(1));
                return p.getPaymentId();
            }
        }
    }

    public void updateStatusByBooking(Connection c, int bookingId, PaymentStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE payments SET status = ? WHERE booking_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }
}
