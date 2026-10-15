package com.goaltracker.dao;

import com.goaltracker.exception.AppException;
import com.goaltracker.model.TimeLog;
import com.goaltracker.util.DBUtil;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class TimeLogDAO implements CrudDao<TimeLog> {

    private static final String BASE =
        "SELECT t.id, t.goal_id, t.start_time, t.end_time, t.duration_minutes, g.name AS goal_name, g.user_id "
      + "FROM time_logs t JOIN goals g ON g.id = t.goal_id ";

    private static final String COMPLETE_IF_REACHED =
        "UPDATE goals SET status = 'COMPLETED' WHERE id = ? "
      + "AND (SELECT COALESCE(SUM(duration_minutes), 0) FROM time_logs WHERE goal_id = ?) >= target_hours * 60";

    private static final String REOPEN_IF_BELOW =
        "UPDATE goals SET status = 'ACTIVE' WHERE id = ? AND status = 'COMPLETED' "
      + "AND (SELECT COALESCE(SUM(duration_minutes), 0) FROM time_logs WHERE goal_id = ?) < target_hours * 60";

    private TimeLog map(ResultSet rs) throws SQLException {
        TimeLog t = new TimeLog();
        t.setId(rs.getInt("id"));
        t.setGoalId(rs.getInt("goal_id"));
        t.setUserId(rs.getInt("user_id"));
        t.setGoalName(rs.getString("goal_name"));
        t.setStart(rs.getTimestamp("start_time").toLocalDateTime());
        t.setEnd(rs.getTimestamp("end_time").toLocalDateTime());
        t.setDurationMinutes(rs.getInt("duration_minutes"));
        return t;
    }

    /** Transaction: insert the log, then mark the goal COMPLETED if the target hours are reached. */
    @Override
    public int create(TimeLog t) throws AppException {
        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = 0;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO time_logs (goal_id, start_time, end_time, duration_minutes) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, t.getGoalId());
                    ps.setTimestamp(2, Timestamp.valueOf(t.getStart()));
                    ps.setTimestamp(3, Timestamp.valueOf(t.getEnd()));
                    ps.setInt(4, t.getDurationMinutes());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) id = keys.getInt(1); }
                }
                updateStatus(c, COMPLETE_IF_REACHED, t.getGoalId());
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("Could not save time log", e);
        }
    }

    /** Transaction: delete the log and re-open the goal if it drops below its target again. */
    @Override
    public boolean delete(int id) throws AppException {
        TimeLog existing = findById(id);
        if (existing == null) return false;
        try (Connection c = DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM time_logs WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                updateStatus(c, REOPEN_IF_BELOW, existing.getGoalId());
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AppException("Could not delete time log", e);
        }
    }

    private void updateStatus(Connection c, String sql, int goalId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.setInt(2, goalId);
            ps.executeUpdate();
        }
    }

    @Override
    public TimeLog findById(int id) throws AppException {
        List<TimeLog> list = query(BASE + "WHERE t.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<TimeLog> findByUser(int userId) throws AppException {
        return query(BASE + "WHERE g.user_id = ? ORDER BY t.start_time DESC", userId);
    }

    @Override
    public List<TimeLog> findAll() throws AppException {
        return query(BASE + "ORDER BY t.start_time DESC");
    }

    private List<TimeLog> query(String sql, Object... params) throws AppException {
        List<TimeLog> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new AppException("Could not load time logs", e);
        }
    }

    // ---------- Admin usage statistics (hours) ----------

    public double totalHours() throws AppException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(duration_minutes), 0) FROM time_logs");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) / 60.0 : 0;
        } catch (SQLException e) {
            throw new AppException("Could not load usage stats", e);
        }
    }

    /** Hours logged per user (top 8), ordered high to low. */
    public Map<String, Double> hoursByUser() throws AppException {
        String sql = "SELECT u.name, COALESCE(SUM(t.duration_minutes), 0) AS m FROM users u "
                   + "LEFT JOIN goals g ON g.user_id = u.id LEFT JOIN time_logs t ON t.goal_id = g.id "
                   + "WHERE u.role = 'USER' GROUP BY u.id, u.name ORDER BY m DESC LIMIT 8";
        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.put(rs.getString("name"), round(rs.getLong("m") / 60.0));
            return result;
        } catch (SQLException e) {
            throw new AppException("Could not load usage stats", e);
        }
    }

    /** Hours logged on each of the last 7 days (days with no activity are filled with 0). */
    public Map<String, Double> hoursLast7Days() throws AppException {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) result.put(LocalDate.now().minusDays(i).toString(), 0.0);
        String sql = "SELECT CAST(start_time AS DATE) AS d, SUM(duration_minutes) AS m FROM time_logs "
                   + "WHERE start_time >= ? GROUP BY CAST(start_time AS DATE)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDate.now().minusDays(6).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.replace(rs.getDate("d").toLocalDate().toString(), round(rs.getLong("m") / 60.0));
            }
            return result;
        } catch (SQLException e) {
            throw new AppException("Could not load usage stats", e);
        }
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }
}
