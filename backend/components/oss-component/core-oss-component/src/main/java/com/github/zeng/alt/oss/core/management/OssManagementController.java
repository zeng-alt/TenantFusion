package com.github.zeng.alt.oss.core.management;

import com.github.zeng.alt.oss.OssConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * OSS 连接管理端点。
 * <p>
 * 提供手动触发 OSS 连接刷新的 REST API。
 * 通过 {@code oss.s3.management.enabled=true} 启用（默认禁用）。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@RestController
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
@ConditionalOnProperty(prefix = "oss.s3.management", name = "enabled", havingValue = "true")
@RequestMapping("/api/oss")
public class OssManagementController {

    private static final Logger log = LoggerFactory.getLogger(OssManagementController.class);

    private final OssConnectionManager connectionManager;

    public OssManagementController(OssConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * 刷新 OSS 连接。
     * <p>
     * 重新创建 S3 客户端连接并原子性替换当前模板，
     * 旧连接在所有正在进行的文件操作完成后自动关闭。
     */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        log.info("Manual OSS connection refresh triggered via API");
        connectionManager.refresh();
        return Map.of(
                "success", true,
                "message", "OSS connection refresh initiated. New connections will be used for subsequent operations."
        );
    }
}
