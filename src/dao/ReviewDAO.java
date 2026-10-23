package dao;

import db.DBConnection;
import db.DataAccessException;
import model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReviewDAO {

    /** Throws DuplicateEntryException if this player already reviewed this turf. */
    public int insert(Review r) {
        String sql = "INSERT INTO reviews (player_id, turf_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getPlayerId());
            ps.setInt(2, r.getTurfId());
            ps.setInt(3, r.getRating());
            ps.setString(4, r.getComment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                r.setReviewId(keys.getInt(1));
                return r.getReviewId();
            }
        } catch (SQLException e) {
            throw DataAccessException.of("Could not save review", e);
        }
    }

    public List<Review> findByTurf(int turfId) {
        String sql = "SELECT r.review_id, r.player_id, r.turf_id, r.rating, r.comment, r.created_at, u.name "
                   + "FROM reviews r JOIN users u ON u.user_id = r.player_id "
                   + "WHERE r.turf_id = ? ORDER BY r.created_at DESC";
        List<Review> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, turfId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review(rs.getInt("review_id"), rs.getInt("player_id"),
                            rs.getInt("turf_id"), rs.getInt("rating"), rs.getString("comment"));
                    r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    r.setPlayerName(rs.getString("name"));
                    list.add(r);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load reviews", e);
        }
    }

    public boolean hasPlayerReviewed(int playerId, int turfId) {
        String sql = "SELECT 1 FROM reviews WHERE player_id = ? AND turf_id = ? LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setInt(2, turfId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check reviews", e);
        }
    }

    public Optional<Review> findByTurfAndPlayer(int turfId, int playerId) {
        String sql = "SELECT r.review_id, r.player_id, r.turf_id, r.rating, r.comment, r.created_at, u.name "
                   + "FROM reviews r JOIN users u ON u.user_id = r.player_id "
                   + "WHERE r.turf_id = ? AND r.player_id = ? LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, turfId);
            ps.setInt(2, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Review r = new Review(rs.getInt("review_id"), rs.getInt("player_id"),
                            rs.getInt("turf_id"), rs.getInt("rating"), rs.getString("comment"));
                    r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    r.setPlayerName(rs.getString("name"));
                    return Optional.of(r);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load review", e);
        }
    }

    public boolean delete(int reviewId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reviews WHERE review_id = ?")) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete review", e);
        }
    }
}
