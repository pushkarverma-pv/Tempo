package com.goaltracker.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Loads db.properties once and hands out JDBC connections.
 * Works with embedded H2 (default) or MySQL; the environment variables DB_URL, DB_USER and
 * DB_PASSWORD override the file, which is how the hosted version is configured.
 */
public final class DBUtil {
    private static final Properties PROPS = new Properties();
    private static final String URL;

    static {
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            PROPS.load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        URL = setting("DB_URL", "db.url");
        try {
            Class.forName(URL.startsWith("jdbc:h2:") ? "org.h2.Driver" : "com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBUtil() { }

    private static String setting(String envName, String propName) {
        String fromEnv = System.getenv(envName);
        return fromEnv != null && !fromEnv.isBlank() ? fromEnv : PROPS.getProperty(propName, "");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, setting("DB_USER", "db.user"), setting("DB_PASSWORD", "db.password"));
    }

    /** True for constraint violations (SQLSTATE class 23), e.g. a duplicate email, on both H2 and MySQL. */
    public static boolean isIntegrityViolation(SQLException e) {
        String state = e.getSQLState();
        return state != null && state.startsWith("23");
    }

    /** Releases the embedded H2 file on shutdown so a redeploy can reopen it. No-op for MySQL. */
    public static void shutdown() {
        if (!URL.startsWith("jdbc:h2:")) return;
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("SHUTDOWN");
        } catch (SQLException ignored) {
            // database already closed
        }
    }
}
