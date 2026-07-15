package com.github.zeng.alt.config.client;

import com.github.zeng.alt.config.model.ConfigItemDTO;
import lombok.extern.apachecommons.CommonsLog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CommonsLog
public class ConfigCacheManager {

    private final Map<String, ConfigItemDTO> cache = new ConcurrentHashMap<>();
    private final Path cacheFile;
    private boolean initialized = false;

    public ConfigCacheManager(String cacheDir, String appCode) {
        if (cacheDir != null && !cacheDir.isBlank()) {
            this.cacheFile = Paths.get(cacheDir, appCode + "-config-cache.json");
        } else {
            String userHome = System.getProperty("user.home", ".");
            this.cacheFile = Paths.get(userHome, ".config-cache", appCode + "-config-cache.json");
        }
        loadFromDisk();
    }

    public void updateAll(List<ConfigItemDTO> items) {
        for (ConfigItemDTO item : items) {
            if (item != null) {
                cache.put(item.getDataId(), item);
            }
        }
        saveToDisk();
        initialized = true;
    }

    public void update(String dataId, ConfigItemDTO item) {
        if (item != null) {
            cache.put(dataId, item);
        } else {
            cache.remove(dataId);
        }
        saveToDisk();
    }

    public ConfigItemDTO get(String dataId) {
        return cache.get(dataId);
    }

    public ConfigItemDTO get(String dataId, String group) {
        return cache.values().stream()
                .filter(c -> c.getDataId().equals(dataId) && c.getGroup().equals(group))
                .findFirst()
                .orElse(null);
    }

    public Map<String, ConfigItemDTO> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(cache));
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void saveToDisk() {
        try {
            Files.createDirectories(cacheFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(cacheFile, StandardCharsets.UTF_8)) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                for (ConfigItemDTO item : cache.values()) {
                    if (!first) json.append(",");
                    json.append(toJson(item));
                    first = false;
                }
                json.append("]");
                writer.write(json.toString());
            }
        } catch (IOException e) {
            log.warn("Failed to save config cache to disk: " + e.getMessage());
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(cacheFile)) {
            return;
        }
        try {
            String content = Files.readString(cacheFile, StandardCharsets.UTF_8);
            if (content.isBlank() || content.equals("[]")) return;

            String trimmed = content.substring(1, content.length() - 1);
            String[] items = splitJsonArray(trimmed);
            for (String item : items) {
                ConfigItemDTO dto = parseJson(item);
                if (dto != null) {
                    cache.put(dto.getDataId(), dto);
                }
            }
            initialized = true;
            log.info("Loaded " + cache.size() + " configs from local cache: " + cacheFile);
        } catch (IOException e) {
            log.warn("Failed to load config cache from disk: " + e.getMessage());
        }
    }

    private String toJson(ConfigItemDTO item) {
        return "{\"configId\":" + item.getConfigId()
                + ",\"dataId\":\"" + escapeJson(item.getDataId())
                + "\",\"group\":\"" + escapeJson(item.getGroup())
                + "\",\"content\":\"" + escapeJson(item.getContent())
                + "\",\"format\":\"" + escapeJson(item.getFormat())
                + "\",\"version\":" + item.getVersion()
                + ",\"appCode\":\"" + escapeJson(item.getAppCode()) + "\"}";
    }

    private ConfigItemDTO parseJson(String json) {
        try {
            ConfigItemDTO dto = new ConfigItemDTO();
            dto.setConfigId(extractLong(json, "configId"));
            dto.setDataId(extractString(json, "dataId"));
            dto.setGroup(extractString(json, "group"));
            dto.setContent(extractString(json, "content"));
            dto.setFormat(extractString(json, "format"));
            dto.setVersion(extractInt(json, "version"));
            dto.setAppCode(extractString(json, "appCode"));
            return dto;
        } catch (Exception e) {
            log.warn("Failed to parse cached config entry: " + e.getMessage());
            return null;
        }
    }

    private String[] splitJsonArray(String trimmed) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') depth++;
            if (c == '}') depth--;
            if (depth == 0 && c == '}') {
                result.add(trimmed.substring(start, i + 1));
                start = i + 2;
            }
        }
        return result.toArray(new String[0]);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String extractString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\":\"");
        if (idx < 0) {
            idx = json.indexOf("\"" + key + "\":");
            if (idx < 0) return "";
            int start = json.indexOf('"', idx + key.length() + 3);
            if (start < 0) return "";
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : "";
        }
        int start = idx + key.length() + 4;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"') { sb.append('"'); i++; }
                else if (next == 'n') { sb.append('\n'); i++; }
                else if (next == 'r') { sb.append('\r'); i++; }
                else if (next == 't') { sb.append('\t'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else sb.append(c);
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private long extractLong(String json, String key) {
        String s = extractString(json, key);
        if (s.isEmpty()) {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx < 0) return 0;
            int start = idx + key.length() + 3;
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            if (end < 0) return 0;
            try { return Long.parseLong(json.substring(start, end).trim()); }
            catch (NumberFormatException e) { return 0; }
        }
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return 0; }
    }

    private int extractInt(String json, String key) {
        return (int) extractLong(json, key);
    }
}
