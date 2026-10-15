package com.goaltracker.filter;

import com.goaltracker.model.Role;
import com.goaltracker.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Protects /user/* and /admin/*: login required, and the role must match the area. */
@WebFilter(urlPatterns = {"/user/*", "/admin/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        String ctx = req.getContextPath();

        if (user == null) { resp.sendRedirect(ctx + "/login"); return; }

        boolean adminArea = req.getServletPath().equals("/admin");
        if (adminArea && user.getRole() != Role.ADMIN) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admins only"); return; }
        if (!adminArea && user.getRole() == Role.ADMIN) { resp.sendRedirect(ctx + "/admin/dashboard"); return; }

        resp.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
