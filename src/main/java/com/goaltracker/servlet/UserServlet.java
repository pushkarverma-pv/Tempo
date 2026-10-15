package com.goaltracker.servlet;

import com.goaltracker.dao.*;
import com.goaltracker.exception.AppException;
import com.goaltracker.exception.ValidationException;
import com.goaltracker.listener.AppInitListener;
import com.goaltracker.model.*;
import com.goaltracker.service.ProgressService;
import com.goaltracker.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** User dashboard: set goals, log time, monitor progress. Routes: GET /user/dashboard, POST /user/goal/add ... */
@WebServlet("/user/*")
public class UserServlet extends HttpServlet {
    private final GoalDAO goals = new GoalDAO();
    private final TimeLogDAO logs = new TimeLogDAO();
    private final GoalTypeDAO types = new GoalTypeDAO();
    private final ProgressService progress = new ProgressService();

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        try {
            List<Goal> myGoals = goals.findByUser(user.getId());
            req.setAttribute("goals", myGoals);
            req.setAttribute("activeGoals", myGoals.stream().filter(g -> !g.isCompleted()).toList());
            req.setAttribute("logs", logs.findByUser(user.getId()));
            req.setAttribute("types", types.findAll());
            req.setAttribute("summary", progress.summarize(myGoals));

            Map<Integer, List<String>> reminders = (Map<Integer, List<String>>) getServletContext().getAttribute(AppInitListener.REMINDERS);
            req.setAttribute("reminders", reminders == null ? List.of() : reminders.getOrDefault(user.getId(), List.of()));
            req.getRequestDispatcher("/WEB-INF/views/user/dashboard.jsp").forward(req, resp);
        } catch (AppException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        HttpSession session = req.getSession();
        try {
            switch (action) {
                case "/goal/add" -> addGoal(req, user, session);
                case "/goal/delete" -> deleteGoal(req, user, session);
                case "/log/add" -> addLog(req, user, session);
                case "/log/delete" -> deleteLog(req, user, session);
                default -> throw new ValidationException("Unknown action");
            }
        } catch (ValidationException e) {
            flash(session, e.getMessage(), "error");
        } catch (AppException e) {
            log("User action failed", e);
            flash(session, "Something went wrong. Please try again.", "error");
        }
        resp.sendRedirect(req.getContextPath() + "/user/dashboard");
    }

    private void addGoal(HttpServletRequest req, User user, HttpSession session) throws AppException {
        int typeId = Validator.requireId(req.getParameter("typeId"), "goal type");
        GoalType type = types.findById(typeId);
        if (type == null) throw new ValidationException("Unknown goal type");

        Goal g = new Goal();
        g.setUserId(user.getId());
        g.setGoalTypeId(typeId);
        g.setName(Validator.requireText(req.getParameter("name"), "Goal name", 100));
        g.setTargetHours(Validator.requirePositive(req.getParameter("targetHours"), "Target hours", type.getMaxTargetHours()));
        g.setDeadline(Validator.requireFutureDate(req.getParameter("deadline")));
        goals.create(g);
        flash(session, "Goal \"" + g.getName() + "\" created.", "ok");
    }

    private void deleteGoal(HttpServletRequest req, User user, HttpSession session) throws AppException {
        Goal g = ownedGoal(Validator.requireId(req.getParameter("id"), "goal"), user);
        goals.delete(g.getId());
        flash(session, "Goal deleted along with its time logs.", "ok");
    }

    private void addLog(HttpServletRequest req, User user, HttpSession session) throws AppException {
        Goal g = ownedGoal(Validator.requireId(req.getParameter("goalId"), "goal"), user);
        if (g.isCompleted()) throw new ValidationException("This goal is already completed");

        LocalDateTime start = Validator.requireDateTime(req.getParameter("start"), "start time");
        LocalDateTime end = Validator.requireDateTime(req.getParameter("end"), "end time");
        if (!end.isAfter(start)) throw new ValidationException("End time must be after start time");
        if (end.isAfter(LocalDateTime.now().plusMinutes(1))) throw new ValidationException("End time cannot be in the future");
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes > 24 * 60) throw new ValidationException("A single entry cannot exceed 24 hours");

        TimeLog t = new TimeLog();
        t.setGoalId(g.getId());
        t.setStart(start);
        t.setEnd(end);
        t.setDurationMinutes((int) minutes);
        logs.create(t);

        boolean done = goals.findById(g.getId()).isCompleted();
        flash(session, done ? "Time logged. Goal \"" + g.getName() + "\" is complete!" : "Time logged.", "ok");
    }

    private void deleteLog(HttpServletRequest req, User user, HttpSession session) throws AppException {
        TimeLog t = logs.findById(Validator.requireId(req.getParameter("id"), "time log"));
        if (t == null || t.getUserId() != user.getId()) throw new ValidationException("Time log not found");
        logs.delete(t.getId());
        flash(session, "Time entry removed.", "ok");
    }

    /** Loads a goal and makes sure it belongs to the signed-in user. */
    private Goal ownedGoal(int id, User user) throws AppException {
        Goal g = goals.findById(id);
        if (g == null || g.getUserId() != user.getId()) throw new ValidationException("Goal not found");
        return g;
    }

    private void flash(HttpSession s, String msg, String type) {
        s.setAttribute("flash", msg);
        s.setAttribute("flashType", type);
    }
}
