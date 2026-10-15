package com.goaltracker.listener;

import com.goaltracker.dao.GoalDAO;
import com.goaltracker.dao.GoalTypeDAO;
import com.goaltracker.dao.UserDAO;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalType;
import com.goaltracker.model.Role;
import com.goaltracker.model.User;
import com.goaltracker.util.DBUtil;
import com.goaltracker.util.PasswordUtil;
import com.goaltracker.util.SchemaInit;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.*;
import java.util.concurrent.*;

/**
 * Runs at application startup: creates the default admin and starts a background thread that
 * refreshes deadline reminders every 30 minutes (Multithreading + concurrent collections).
 */
@WebListener
public class AppInitListener implements ServletContextListener {
    public static final String REMINDERS = "reminders";
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        try {
            SchemaInit.run();
            seedGoalTypes();
            ctx.log("[Tempo] Database ready.");
        } catch (Exception e) {
            ctx.log("[Tempo] DATABASE SETUP FAILED.", e);
        }
        createDefaultAdmin(ctx);

        Map<Integer, List<String>> reminders = new ConcurrentHashMap<>();
        ctx.setAttribute(REMINDERS, reminders);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "deadline-reminder");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> refresh(reminders), 0, 30, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) scheduler.shutdownNow();
        DBUtil.shutdown();
    }

    /** Inserts the starter goal types the first time the app runs against an empty database. */
    private void seedGoalTypes() throws Exception {
        GoalTypeDAO dao = new GoalTypeDAO();
        if (!dao.findAll().isEmpty()) return;
        String[][] seeds = {
            {"Study", "Hours of focused study", "300"},
            {"Fitness", "Hours of training", "200"},
            {"Career", "Hours of skill building", "400"},
            {"Personal", "Hours spent on the goal", "150"}
        };
        for (String[] seed : seeds) {
            GoalType t = new GoalType();
            t.setName(seed[0]);
            t.setMetric(seed[1]);
            t.setMaxTargetHours(Integer.parseInt(seed[2]));
            dao.create(t);
        }
    }

    private void createDefaultAdmin(ServletContext ctx) {
        try {
            UserDAO dao = new UserDAO();
            if (!dao.adminExists()) {
                User admin = new User();
                admin.setName("Administrator");
                admin.setEmail("admin@tempo.app");
                String envPassword = System.getenv("ADMIN_PASSWORD");   // set this when hosting publicly
                boolean custom = envPassword != null && envPassword.length() >= 6;
                admin.setPasswordHash(PasswordUtil.hash(custom ? envPassword : "Admin@123"));
                admin.setRole(Role.ADMIN);
                dao.create(admin);
                ctx.log("[Tempo] Default admin created: admin@tempo.app");
            }
        } catch (Exception e) {
            ctx.log("[Tempo] Could not create default admin.", e);
        }
    }

    private void refresh(Map<Integer, List<String>> reminders) {
        try {
            Map<Integer, List<String>> fresh = new HashMap<>();
            for (Goal g : new GoalDAO().findDueSoon(3)) {
                String when = g.getDaysLeft() == 0 ? "today" : "in " + g.getDaysLeft() + " day(s)";
                fresh.computeIfAbsent(g.getUserId(), k -> new ArrayList<>())
                     .add("\"" + g.getName() + "\" is due " + when + " (" + g.getProgressPercent() + "% done)");
            }
            reminders.keySet().retainAll(fresh.keySet());
            fresh.forEach((userId, list) -> reminders.put(userId, List.copyOf(list)));
        } catch (Exception e) {
            System.err.println("[Tempo] Reminder refresh failed: " + e.getMessage());
        }
    }
}
