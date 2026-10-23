package dao;

import db.DBConnection;
import db.DataAccessException;
import model.TimeSlot;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimeSlotDAO {

    private static final String COLUMNS = "slot_id, turf_id, slot_date, start_time, end_time, is_booked";

    /** Throws DuplicateEntryException if the turf already has a slot starting at that date and time. */
    public int insert(TimeSlot s) {
        String sql = "INSERT INTO time_slots (turf_id, slot_date, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getTurfId());
            ps.setDate(2, Date.valueOf(s.getDate()));
            ps.setTime(3, Time.valueOf(s.getStartTime()));
            ps.setTime(4, Time.valueOf(s.getEndTime()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                s.setSlotId(keys.getInt(1));
                return s.getSlotId();
            }
        } catch (SQLException e) {
            throw DataAccessException.of("Could not save slot", e);
        }
    }

    /** Upcoming slots of a turf, optionally only the free ones. */
    public List<TimeSlot> findByTurf(int turfId, boolean onlyAvailable) {
        String sql = "SELECT " + COLUMNS + " FROM time_slots WHERE turf_id = ? AND slot_date >= CURDATE()"
                   + (onlyAvailable ? " AND is_booked = FALSE" : "")
                   + " ORDER BY slot_date, start_time";
        List<TimeSlot> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, turfId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load slots", e);
        }
    }

    public Optional<TimeSlot> findById(int slotId) {
        try (Connection c = DBConnection.getConnection()) {
            return findById(c, slotId);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load slot", e);
        }
    }

    public Optional<TimeSlot> findById(Connection c, int slotId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM time_slots WHERE slot_id = ?")) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Reserves the slot in ONE atomic statement. If two players click at the same moment,
     * MySQL lets only one UPDATE match 'is_booked = FALSE'; the other gets 0 rows and returns false.
     */
    public boolean markBooked(Connection c, int slotId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE time_slots SET is_booked = TRUE WHERE slot_id = ? AND is_booked = FALSE")) {
            ps.setInt(1, slotId);
            return ps.executeUpdate() == 1;
        }
    }

    public void release(Connection c, int slotId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE time_slots SET is_booked = FALSE WHERE slot_id = ?")) {
            ps.setInt(1, slotId);
            ps.executeUpdate();
        }
    }

    public boolean delete(int slotId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM time_slots WHERE slot_id = ? AND is_booked = FALSE")) {
            ps.setInt(1, slotId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete slot", e);
        }
    }

    private TimeSlot map(ResultSet rs) throws SQLException {
        return new TimeSlot(rs.getInt("slot_id"), rs.getInt("turf_id"),
                rs.getDate("slot_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getBoolean("is_booked"));
    }
}
