package com.goaltracker.dao;

import com.goaltracker.exception.AppException;
import com.goaltracker.exception.ValidationException;
import com.goaltracker.model.Role;
import com.goaltracker.model.User;
import com.goaltracker.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO implements CrudDao<User> {

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        return u;
    }

    @Override
    public int create(User u) throws AppException {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRole().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : 0; }
        } catch (SQLException e) {
            if (DBUtil.isIntegrityViolation(e)) throw new ValidationException("That email is already registered");
            throw new AppException("Could not create user", e);
        }
    }

    @Override
    public User findById(int id) throws AppException {
        return findOne("SELECT * FROM users WHERE id = ?", String.valueOf(id));
    }

    public User findByEmail(String email) throws AppException {
        return findOne("SELECT * FROM users WHERE email = ?", email);
    }

    private User findOne(String sql, String param) throws AppException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) {
            throw new AppException("Could not load user", e);
        }
    }

    @Override
    public List<User> findAll() throws AppException {
        List<User> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new AppException("Could not load users", e);
        }
    }

    public boolean adminExists() throws AppException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new AppException("Could not check admin account", e);
        }
    }

    public boolean setActive(int id, boolean active) throws AppException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET active = ? WHERE id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AppException("Could not update user", e);
        }
    }

    /** Transaction: a user's time logs, goals and the user row are removed together or not at all. */
    @Override
    public boolean delete(int id) throws AppException {
        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement logs = c.prepareStatement("DELETE FROM time_logs WHERE goal_id IN (SELECT id FROM goals WHERE user_id = ?)");
                 PreparedStatement goals = c.prepareStatement("DELETE FROM goals WHERE user_id = ?");
                 PreparedStatement user = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
                logs.setInt(1, id);  logs.executeUpdate();
                goals.setInt(1, id); goals.executeUpdate();
                user.setInt(1, id);
                int rows = user.executeUpdate();
                c.commit();
                return rows > 0;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("Could not delete user", e);
        }
    }
}
