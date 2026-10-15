package com.goaltracker.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Runs schema.sql (from the classpath) at startup so no manual database setup is needed. */
public final class SchemaInit {
    private SchemaInit() { }

    public static void run() throws SQLException, IOException {
        List<String> statements = loadStatements("/schema.sql");
        try (Connection c = DBUtil.getConnection(); Statement st = c.createStatement()) {
            for (String sql : statements) st.execute(sql);
        }
    }

    /** Splits the script on semicolons, ignoring "--" comment lines. */
    static List<String> loadStatements(String resource) throws IOException {
        StringBuilder script = new StringBuilder();
        try (InputStream in = SchemaInit.class.getResourceAsStream(resource)) {
            if (in == null) throw new IOException(resource + " not found on classpath");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().startsWith("--")) script.append(line).append('\n');
            }
        }
        List<String> result = new ArrayList<>();
        for (String part : script.toString().split(";")) {
            if (!part.isBlank()) result.add(part.trim());
        }
        return result;
    }
}
