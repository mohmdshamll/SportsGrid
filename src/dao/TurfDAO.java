package dao;

import db.DBConnection;
import db.DataAccessException;
import model.Turf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TurfDAO {

    // average rating is calculated from the reviews table every time
    private static final String SELECT =
            "SELECT t.turf_id, t.owner_id, u.name AS owner_name, t.name, t.location, t.sport_type, t.price_per_hour, t.is_approved, "
          + "COALESCE(AVG(r.rating), 0) AS avg_rating "
          + "FROM turfs t "
          + "JOIN users u ON u.user_id = t.owner_id "
          + "LEFT JOIN reviews r ON r.turf_id = t.turf_id ";
    private static final String GROUP =
            " GROUP BY t.turf_id, t.owner_id, u.name, t.name, t.location, t.sport_type, t.price_per_hour, t.is_approved "
          + "ORDER BY t.name";

    public int insert(Turf turf) {
        String sql = "INSERT INTO turfs (owner_id, name, location, sport_type, price_per_hour, is_approved) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, turf.getOwnerId());
            ps.setString(2, turf.getName());
            ps.setString(3, turf.getLocation());
            ps.setString(4, turf.getSportType());
            ps.setDouble(5, turf.getPricePerHour());
            ps.setBoolean(6, turf.isApproved());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                turf.setTurfId(keys.getInt(1));
                return turf.getTurfId();
            }
        } catch (SQLException e) {
            throw DataAccessException.of("Could not save turf", e);
        }
    }

    public void update(Turf turf) {
        String sql = "UPDATE turfs SET name = ?, location = ?, sport_type = ?, price_per_hour = ? WHERE turf_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, turf.getName());
            ps.setString(2, turf.getLocation());
            ps.setString(3, turf.getSportType());
            ps.setDouble(4, turf.getPricePerHour());
            ps.setInt(5, turf.getTurfId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update turf", e);
        }
    }

    public Optional<Turf> findById(int turfId) {
        List<Turf> list = query("WHERE t.turf_id = ?", turfId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Approved turfs only; sport and location are optional filters (null or blank = no filter). */
    public List<Turf> search(String sport, String location) {
        StringBuilder where = new StringBuilder("WHERE t.is_approved = TRUE");
        List<Object> params = new ArrayList<>();
        if (sport != null && !sport.isBlank()) {
            where.append(" AND t.sport_type LIKE ?");
            params.add("%" + sport.trim() + "%");
        }
        if (location != null && !location.isBlank()) {
            where.append(" AND t.location LIKE ?");
            params.add("%" + location.trim() + "%");
        }
        return query(where.toString(), params.toArray());
    }

    public List<Turf> findAll() { return query(""); }

    public List<Turf> findByOwner(int ownerId) { return query("WHERE t.owner_id = ?", ownerId); }

    public List<Turf> findPending() { return query("WHERE t.is_approved = FALSE"); }

    public void setApproved(int turfId, boolean approved) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE turfs SET is_approved = ? WHERE turf_id = ?")) {
            ps.setBoolean(1, approved);
            ps.setInt(2, turfId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update turf approval", e);
        }
    }

    public boolean delete(int turfId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM turfs WHERE turf_id = ?")) {
            ps.setInt(1, turfId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete turf", e);
        }
    }

    private List<Turf> query(String where, Object... params) {
        List<Turf> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + where + GROUP)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Turf t = new Turf(rs.getInt("turf_id"), rs.getInt("owner_id"), rs.getString("name"),
                            rs.getString("location"), rs.getString("sport_type"),
                            rs.getDouble("price_per_hour"), rs.getBoolean("is_approved"));
                    t.setOwnerName(rs.getString("owner_name"));
                    t.setAverageRating(rs.getDouble("avg_rating"));
                    list.add(t);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load turfs", e);
        }
    }
}
