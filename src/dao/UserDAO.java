package dao;

import db.DBConnection;
import db.DataAccessException;
import model.Role;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO = Data Access Object. The only class that talks SQL for the users table. */
public class UserDAO {

    private static final String COLUMNS = "user_id, name, email, phone, password_hash, role";

    /** Saves a new user and returns the generated id. Throws DuplicateEntryException if the email exists. */
    public int insert(User user) {
        String sql = "INSERT INTO users (name, email, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                user.setUserId(id);
                return id;
            }
        } catch (SQLException e) {
            throw DataAccessException.of("Could not save user", e);
        }
    }

    public Optional<User> findByEmail(String email) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE email = ?", email);
    }

    public Optional<User> findById(int userId) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE user_id = ?", userId);
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM users ORDER BY user_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load users", e);
        }
    }

    /** Returns true if at least one user with role ADMIN exists in the database. */
    public boolean hasAdmin() {
        String sql = "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            throw new DataAccessException("Could not check admin users", e);
        }
    }

    /** Returns true if a row was deleted. Turfs/bookings/reviews of that user go too (ON DELETE CASCADE). */
    public boolean delete(int userId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete user", e);
        }
    }

    private Optional<User> findOne(String sql, Object param) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return User.create(Role.valueOf(rs.getString("role")),
                rs.getInt("user_id"), rs.getString("name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("password_hash"));
    }
}
