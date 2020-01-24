package ru.openfs.druid;

import java.util.HashMap;
import java.util.Map;

public class SimpleMap {
    private final Map<String, String> map;

    public SimpleMap(String mapping) {
        String[] uplinks = mapping.split(",");
        this.map = new HashMap<String, String>(uplinks.length / 2);
        for (int i = 0; i < uplinks.length; i++) {
            this.map.put(uplinks[i], uplinks[++i]);
        }
    }

    public String get(String key) {
        if (this.map.containsKey(key)) {
            return this.map.get(key);
        }
        return key;
    }

}