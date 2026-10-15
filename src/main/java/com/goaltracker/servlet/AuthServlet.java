package com.goaltracker.servlet;

import com.goaltracker.dao.UserDAO;
import com.goaltracker.exception.AppException;
import com.goaltracker.exception.ValidationException;
import com.goaltracker.model.Role;
import com.goaltracker.model.User;
import com.goaltracker.util.PasswordUtil;
import com.goaltracker.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Handles /login, /register and /logout. */
@WebServlet({"/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {
    private final UserDAO users = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getServletPath()) {
            case "/logout" -> {
                HttpSession s = req.getSession(false);
                if (s != null) s.invalidate();
                resp.sendRedirect(req.getContextPath() + "/login");
            }
            case "/register" -> req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
            default -> {
                HttpSession s = req.getSession(false);
                User u = s == null ? null : (User) s.getAttribute("user");
                if (u != null) redirectHome(req, resp, u);
                else req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/register".equals(req.getServletPath())) register(req, resp); else login(req, resp);
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String email = req.getParameter("email") == null ? "" : req.getParameter("email").trim().toLowerCase();
            User u = users.findByEmail(email);
            if (u == null || !PasswordUtil.verify(req.getParameter("password"), u.getPasswordHash()))
                throw new ValidationException("Invalid email or password");
            if (!u.isActive()) throw new ValidationException("This account has been deactivated. Contact the admin.");

            u.setPasswordHash(null);                 // never keep the hash in the session
            req.getSession(true);
            req.changeSessionId();                   // prevents session fixation
            HttpSession session = req.getSession();
            session.setAttribute("user", u);
            session.setMaxInactiveInterval(30 * 60);
            redirectHome(req, resp, u);
        } catch (AppException e) {
            req.setAttribute("error", e instanceof ValidationException ? e.getMessage() : "Server error. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
        }
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            User u = new User();
            u.setName(Validator.requireText(req.getParameter("name"), "Name", 100));
            u.setEmail(Validator.requireEmail(req.getParameter("email")));
            String pw = Validator.requirePassword(req.getParameter("password"));
            if (!pw.equals(req.getParameter("confirm"))) throw new ValidationException("Passwords do not match");
            u.setPasswordHash(PasswordUtil.hash(pw));
            u.setRole(Role.USER);
            users.create(u);
            req.setAttribute("success", "Account created. You can sign in now.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
        } catch (AppException e) {
            req.setAttribute("error", e instanceof ValidationException ? e.getMessage() : "Server error. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, resp);
        }
    }

    private void redirectHome(HttpServletRequest req, HttpServletResponse resp, User u) throws IOException {
        resp.sendRedirect(req.getContextPath() + (u.getRole() == Role.ADMIN ? "/admin/dashboard" : "/user/dashboard"));
    }
}
