package com.goaltracker.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonUtilTest {

    @Test
    void buildsLabelsAndValuesInOrder() {
        Map<String, Double> data = new LinkedHashMap<>();
        data.put("Asha", 3.5);
        data.put("Ravi", 1.0);
        assertEquals("{\"labels\":[\"Asha\",\"Ravi\"],\"values\":[3.5,1.0]}", JsonUtil.chart(data));
    }

    @Test
    void emptyDataGivesEmptyArrays() {
        assertEquals("{\"labels\":[],\"values\":[]}", JsonUtil.chart(new LinkedHashMap<String, Integer>()));
    }

    @Test
    void escapesQuotesAndScriptTags() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("a\"b</script>", 1);
        String json = JsonUtil.chart(data);
        assertFalse(json.contains("</script>"));
        assertTrue(json.contains("\\\""));
    }
}
