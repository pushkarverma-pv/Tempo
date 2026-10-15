package com.goaltracker.dao;

import com.goaltracker.exception.AppException;
import com.goaltracker.exception.ValidationException;
import com.goaltracker.model.GoalType;
import com.goaltracker.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GoalTypeDAO implements CrudDao<GoalType> {

    private GoalType map(ResultSet rs) throws SQLException {
        GoalType t = new GoalType();
        t.setId(rs.getInt("id"));
        t.setName(rs.getString("name"));
        t.setMetric(rs.getString("metric"));
        t.setMaxTargetHours(rs.getInt("max_target_hours"));
        return t;
    }

    @Override
    public int create(GoalType t) throws AppException {
        String sql = "INSERT INTO goal_types (name, metric, max_target_hours) VALUES (?, ?, ?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getMetric());
            ps.setInt(3, t.getMaxTargetHours());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : 0; }
        } catch (SQLException e) {
            if (DBUtil.isIntegrityViolation(e)) throw new ValidationException("A goal type with that name already exists");
            throw new AppException("Could not save goal type", e);
        }
    }

    @Override
    public GoalType findById(int id) throws AppException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM goal_types WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        } catch (SQLException e) {
            throw new AppException("Could not load goal type", e);
        }
    }

    @Override
    public List<GoalType> findAll() throws AppException {
        List<GoalType> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM goal_types ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new AppException("Could not load goal types", e);
        }
    }

    @Override
    public boolean delete(int id) throws AppException {
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM goal_types WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AppException("Could not delete goal type", e);
        }
    }
}
