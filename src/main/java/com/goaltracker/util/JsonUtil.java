package com.goaltracker.util;

import java.util.Map;

/** Tiny JSON builder for chart data: {"labels":[...],"values":[...]}. */
public final class JsonUtil {
    private JsonUtil() { }

    public static String chart(Map<String, ? extends Number> data) {
        StringBuilder labels = new StringBuilder("["), values = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, ? extends Number> e : data.entrySet()) {
            if (!first) { labels.append(','); values.append(','); }
            first = false;
            labels.append('"').append(escape(e.getKey())).append('"');
            values.append(e.getValue());
        }
        return "{\"labels\":" + labels.append(']') + ",\"values\":" + values.append(']') + "}";
    }

    private static String escape(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '<' -> b.append("\\u003c");
                case '>' -> b.append("\\u003e");
                default -> { if (c >= 0x20) b.append(c); }
            }
        }
        return b.toString();
    }
}
