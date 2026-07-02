package com.github.zeng.alt.oss;

import java.time.LocalDateTime;

/**
 * OSS 文件记录持久化服务接口。
 * <p>
 * 当引入 {@code jpa-oss-component} 或 {@code jdbc-oss-component} 等持久化模块时，
 * 其实现会自动与 {@link OssTemplate} 集成，在上传/删除等操作时同步记录文件元数据到数据库。
 * <p>
 * 引入持久化模块后：
 * <ul>
 *   <li>{@code OssTemplate.upload()} → 上传文件 + 自动写入记录</li>
 *   <li>{@code OssTemplate.delete()} → 删除文件 + 自动标记/删除记录</li>
 * </ul>
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public interface OssFileRecordService {

    /**
     * 保存文件上传记录。
     *
     * @param info            上传返回的文件信息
     * @param originalFileName 原始文件名
     */
    void saveRecord(OssFileInfo info, String originalFileName);

    /**
     * 根据文件名标记文件记录为已删除（软删除）。
     *
     * @param fileName 文件名
     */
    void markDeletedByFileName(String fileName);

    /**
     * 清理指定时间之前创建的过期文件记录。
     *
     * @param before 时间界限
     */
    void cleanUp(LocalDateTime before);

    /**
     * 根据 MD5 和用户查找已存在的有效文件记录（用于去重）。
     *
     * @param md5    文件 MD5 哈希
     * @param userId 用户标识（createdBy）
     * @return 已有的文件信息，不存在返回 {@code null}
     */
    OssFileInfo findExistingByMd5(String md5, String userId);
}
