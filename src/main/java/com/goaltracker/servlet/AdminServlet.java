package com.goaltracker.servlet;

import com.goaltracker.dao.*;
import com.goaltracker.exception.AppException;
import com.goaltracker.exception.ValidationException;
import com.goaltracker.model.*;
import com.goaltracker.util.JsonUtil;
import com.goaltracker.util.PasswordUtil;
import com.goaltracker.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

/** Admin dashboard: goal parameters, user accounts, usage metrics. */
@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {
    private final UserDAO users = new UserDAO();
    private final GoalTypeDAO types = new GoalTypeDAO();
    private final GoalDAO goals = new GoalDAO();
    private final TimeLogDAO logs = new TimeLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Goal> allGoals = goals.findAll();
            req.setAttribute("users", users.findAll());
            req.setAttribute("types", types.findAll());
            req.setAttribute("goalCount", allGoals.size());
            req.setAttribute("completedCount", allGoals.stream().filter(Goal::isCompleted).count());
            req.setAttribute("totalHours", Math.round(logs.totalHours() * 10) / 10.0);
            req.setAttribute("chartByUser", JsonUtil.chart(logs.hoursByUser()));
            req.setAttribute("chartByDay", JsonUtil.chart(logs.hoursLast7Days()));
            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
        } catch (AppException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User admin = (User) req.getSession().getAttribute("user");
        HttpSession session = req.getSession();
        String action = req.getPathInfo() == null ? "" : req.getPathInfo();
        try {
            switch (action) {
                case "/type/add" -> {
                    GoalType t = new GoalType();
                    t.setName(Validator.requireText(req.getParameter("name"), "Goal type name", 60));
                    t.setMetric(Validator.requireText(req.getParameter("metric"), "Tracking metric", 100));
                    t.setMaxTargetHours((int) Validator.requirePositive(req.getParameter("maxHours"), "Max target hours", 2000));
                    types.create(t);
                    flash(session, "Goal type \"" + t.getName() + "\" saved. Parameters configured successfully.", "ok");
                }
                case "/type/delete" -> {
                    types.delete(Validator.requireId(req.getParameter("id"), "goal type"));
                    flash(session, "Goal type removed.", "ok");
                }
                case "/user/add" -> {
                    User u = new User();
                    u.setName(Validator.requireText(req.getParameter("name"), "Name", 100));
                    u.setEmail(Validator.requireEmail(req.getParameter("email")));
                    u.setPasswordHash(PasswordUtil.hash(Validator.requirePassword(req.getParameter("password"))));
                    u.setRole("ADMIN".equals(req.getParameter("role")) ? Role.ADMIN : Role.USER);
                    users.create(u);
                    flash(session, "Account created for " + u.getName() + ".", "ok");
                }
                case "/user/toggle" -> {
                    User target = otherUser(req, admin);
                    users.setActive(target.getId(), !target.isActive());
                    flash(session, target.getName() + (target.isActive() ? " deactivated." : " reactivated."), "ok");
                }
                case "/user/delete" -> {
                    User target = otherUser(req, admin);
                    users.delete(target.getId());
                    flash(session, target.getName() + " and all their goals and logs were deleted.", "ok");
                }
                default -> throw new ValidationException("Unknown action");
            }
        } catch (ValidationException e) {
            flash(session, e.getMessage(), "error");
        } catch (AppException e) {
            log("Admin action failed", e);
            flash(session, "Something went wrong. Please try again.", "error");
        }
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }

    /** Finds the user from the form and refuses to act on the admin's own account. */
    private User otherUser(HttpServletRequest req, User admin) throws AppException {
        User target = users.findById(Validator.requireId(req.getParameter("id"), "user"));
        if (target == null) throw new ValidationException("User not found");
        if (target.getId() == admin.getId()) throw new ValidationException("You cannot do that to your own account");
        return target;
    }

    private void flash(HttpSession s, String msg, String type) {
        s.setAttribute("flash", msg);
        s.setAttribute("flashType", type);
    }
}
