package com.goaltracker.dao;

import com.goaltracker.exception.AppException;
import com.goaltracker.model.Goal;
import com.goaltracker.util.DBUtil;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GoalDAO implements CrudDao<Goal> {

    /** Goals joined with their type name and total logged minutes. */
    private static final String BASE =
        "SELECT g.id, g.user_id, g.goal_type_id, g.name, g.target_hours, g.deadline, g.status, gt.name AS type_name, "
      + "COALESCE((SELECT SUM(t.duration_minutes) FROM time_logs t WHERE t.goal_id = g.id), 0) AS logged "
      + "FROM goals g LEFT JOIN goal_types gt ON gt.id = g.goal_type_id ";

    private Goal map(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setUserId(rs.getInt("user_id"));
        g.setGoalTypeId(rs.getInt("goal_type_id"));
        g.setName(rs.getString("name"));
        g.setTargetHours(rs.getDouble("target_hours"));
        g.setDeadline(rs.getDate("deadline").toLocalDate());
        g.setStatus(rs.getString("status"));
        g.setTypeName(rs.getString("type_name"));
        g.setLoggedMinutes(rs.getLong("logged"));
        return g;
    }

    @Override
    public int create(Goal g) throws AppException {
        String sql = "INSERT INTO goals (user_id, goal_type_id, name, target_hours, deadline) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, g.getUserId());
            if (g.getGoalTypeId() == 0) ps.setNull(2, Types.INTEGER); else ps.setInt(2, g.getGoalTypeId());
            ps.setString(3, g.getName());
            ps.setDouble(4, g.getTargetHours());
            ps.setDate(5, Date.valueOf(g.getDeadline()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : 0; }
        } catch (SQLException e) {
            throw new AppException("Could not create goal", e);
        }
    }

    @Override
    public Goal findById(int id) throws AppException {
        List<Goal> list = query(BASE + "WHERE g.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Goal> findByUser(int userId) throws AppException {
        return query(BASE + "WHERE g.user_id = ? ORDER BY g.status, g.deadline", userId);
    }

    @Override
    public List<Goal> findAll() throws AppException {
        return query(BASE + "ORDER BY g.id");
    }

    /** Active goals whose deadline falls within the next {@code days} days (used by the reminder thread). */
    public List<Goal> findDueSoon(int days) throws AppException {
        LocalDate today = LocalDate.now();
        return query(BASE + "WHERE g.status = 'ACTIVE' AND g.deadline BETWEEN ? AND ?",
                Date.valueOf(today), Date.valueOf(today.plusDays(days)));
    }

    private List<Goal> query(String sql, Object... params) throws AppException {
        List<Goal> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new AppException("Could not load goals", e);
        }
    }

    @Override
    public boolean delete(int id) throws AppException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM goals WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;   // time_logs rows go with it (ON DELETE CASCADE)
        } catch (SQLException e) {
            throw new AppException("Could not delete goal", e);
        }
    }
}
