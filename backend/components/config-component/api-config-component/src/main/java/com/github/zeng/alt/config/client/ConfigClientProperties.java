package com.github.zeng.alt.config.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "config.client")
public class ConfigClientProperties {

    private boolean enabled = true;
    private String serverAddr = "http://localhost:8085";
    private String appCode;
    private long longPollTimeoutMs = 30000;
    private String cacheDir;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public long getLongPollTimeoutMs() {
        return longPollTimeoutMs;
    }

    public void setLongPollTimeoutMs(long longPollTimeoutMs) {
        this.longPollTimeoutMs = longPollTimeoutMs;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }
}
