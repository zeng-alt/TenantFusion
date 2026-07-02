package com.github.zeng.alt.oss;

/**
 * OSS 连接管理器。
 * <p>
 * 负责管理 {@link OssTemplate} 的生命周期，支持在运行时动态刷新 OSS 连接
 * （更换 endpoint、accessKey、bucketName 等配置），且不影响正在进行的文件操作。
 * <p>
 * 刷新流程：
 * <ol>
 *   <li>根据新配置创建新的 S3 客户端连接</li>
 *   <li>原子性地将新连接切换到 {@link OssTemplate} 代理中</li>
 *   <li>优雅关闭旧连接（等待正在进行的请求完成）</li>
 * </ol>
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public interface OssConnectionManager {

    /**
     * 获取当前活跃的 OSS 操作模板。
     *
     * @return OSS 操作模板
     */
    OssTemplate getTemplate();

    /**
     * 动态刷新 OSS 连接。
     * <p>
     * 使用更新后的配置重新创建 S3 客户端并替换当前连接。
     * 旧连接会在所有正在进行的请求完成后被优雅关闭。
     * 刷新后，通过 {@link #getTemplate()} 获取的模板会自动指向新连接。
     */
    void refresh();

    /**
     * 销毁所有 OSS 连接，释放资源。
     * <p>
     * 调用此方法后，当前 {@link OssTemplate} 不再可用。
     */
    void destroy();
}
