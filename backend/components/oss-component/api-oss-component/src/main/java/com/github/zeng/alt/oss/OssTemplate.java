package com.github.zeng.alt.oss;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * OSS 操作模板。
 * <p>
 * 统一的对象存储服务操作接口，基于 AWS S3 协议实现。
 * 支持文件上传、下载、删除、查询、复制等操作。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public interface OssTemplate {

    // ==================== 上传 ====================

    /**
     * 上传文件（流式）
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名（含路径）
     * @return 文件信息
     */
    OssFileInfo upload(InputStream inputStream, String fileName);

    /**
     * 上传文件（流式，指定 contentType）
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名（含路径）
     * @param contentType 内容类型，{@code null} 则自动探测
     * @return 文件信息
     */
    OssFileInfo upload(InputStream inputStream, String fileName, String contentType);

    /**
     * 上传文件（字节数组）
     *
     * @param data     文件字节数组
     * @param fileName 文件名（含路径）
     * @return 文件信息
     */
    OssFileInfo upload(byte[] data, String fileName);

    /**
     * 上传文件（字节数组，指定 contentType）
     *
     * @param data        文件字节数组
     * @param fileName    文件名（含路径）
     * @param contentType 内容类型，{@code null} 则自动探测
     * @return 文件信息
     */
    OssFileInfo upload(byte[] data, String fileName, String contentType);

    /**
     * 上传本地文件
     *
     * @param file     本地文件
     * @param fileName 文件名（含路径）
     * @return 文件信息
     */
    OssFileInfo upload(File file, String fileName);

    // ==================== 下载 ====================

    /**
     * 下载文件
     *
     * @param fileName 文件名（含路径）
     * @return 文件输入流
     */
    InputStream download(String fileName);

    // ==================== 删除 ====================

    /**
     * 删除文件
     *
     * @param fileName 文件名（含路径）
     */
    void delete(String fileName);

    /**
     * 批量删除文件
     *
     * @param fileNames 文件名列表（含路径）
     */
    void delete(List<String> fileNames);

    // ==================== 查询 ====================

    /**
     * 判断文件是否存在
     *
     * @param fileName 文件名（含路径）
     * @return true 存在
     */
    boolean exists(String fileName);

    /**
     * 获取文件信息
     *
     * @param fileName 文件名（含路径）
     * @return 文件信息，不存在返回 {@code null}
     */
    OssFileInfo getFileInfo(String fileName);

    /**
     * 获取文件访问 URL
     *
     * @param fileName 文件名（含路径）
     * @return 访问 URL
     */
    String getUrl(String fileName);

    /**
     * 获取文件列表
     *
     * @param prefix 路径前缀
     * @return 文件信息列表
     */
    List<OssFileInfo> listFiles(String prefix);

    // ==================== 其他 ====================

    /**
     * 复制文件
     *
     * @param sourceFileName 源文件名（含路径）
     * @param targetFileName 目标文件名（含路径）
     */
    void copy(String sourceFileName, String targetFileName);

    /**
     * 移动文件（复制后删除源文件）
     *
     * @param sourceFileName 源文件名（含路径）
     * @param targetFileName 目标文件名（含路径）
     */
    void move(String sourceFileName, String targetFileName);

    /**
     * 生成预签名 URL（临时授权访问）
     *
     * @param fileName   文件名（含路径）
     * @param expiration 过期时间（秒）
     * @return 预签名 URL
     */
    String presignedGetUrl(String fileName, int expiration);

    // ==================== 桶感知操作（用于自动桶策略） ====================

    /**
     * 上传文件到指定桶（流式）。
     *
     * @param bucketName  存储桶名称
     * @param inputStream 文件输入流
     * @param fileName    文件名（含路径）
     * @return 文件信息
     */
    default OssFileInfo upload(String bucketName, InputStream inputStream, String fileName) {
        throw new UnsupportedOperationException("Bucket-aware upload not implemented");
    }

    /**
     * 上传文件到指定桶（流式，指定 contentType）。
     *
     * @param bucketName  存储桶名称
     * @param inputStream 文件输入流
     * @param fileName    文件名（含路径）
     * @param contentType 内容类型
     * @return 文件信息
     */
    default OssFileInfo upload(String bucketName, InputStream inputStream, String fileName, String contentType) {
        throw new UnsupportedOperationException("Bucket-aware upload not implemented");
    }

    /**
     * 上传文件到指定桶（字节数组）。
     *
     * @param bucketName 存储桶名称
     * @param data       文件字节数组
     * @param fileName   文件名（含路径）
     * @return 文件信息
     */
    default OssFileInfo upload(String bucketName, byte[] data, String fileName) {
        throw new UnsupportedOperationException("Bucket-aware upload not implemented");
    }

    /**
     * 上传文件到指定桶（字节数组，指定 contentType）。
     *
     * @param bucketName  存储桶名称
     * @param data        文件字节数组
     * @param fileName    文件名（含路径）
     * @param contentType 内容类型
     * @return 文件信息
     */
    default OssFileInfo upload(String bucketName, byte[] data, String fileName, String contentType) {
        throw new UnsupportedOperationException("Bucket-aware upload not implemented");
    }

    /**
     * 判断指定桶中文件是否存在。
     *
     * @param bucketName 存储桶名称
     * @param fileName   文件名（含路径）
     * @return true 存在
     */
    default boolean exists(String bucketName, String fileName) {
        throw new UnsupportedOperationException("Bucket-aware exists check not implemented");
    }

    /**
     * 删除指定桶中的文件。
     *
     * @param bucketName 存储桶名称
     * @param fileName   文件名（含路径）
     */
    default void delete(String bucketName, String fileName) {
        throw new UnsupportedOperationException("Bucket-aware delete not implemented");
    }

    /**
     * 确保指定桶存在，不存在则创建。
     *
     * @param bucketName 存储桶名称
     */
    default void ensureBucketExists(String bucketName) {
        throw new UnsupportedOperationException("ensureBucketExists not implemented");
    }
}
