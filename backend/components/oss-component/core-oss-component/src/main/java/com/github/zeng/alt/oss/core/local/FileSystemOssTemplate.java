package com.github.zeng.alt.oss.core.local;

import com.github.zeng.alt.oss.OssFileInfo;
import com.github.zeng.alt.oss.OssProperties;
import com.github.zeng.alt.oss.OssTemplate;
import com.github.zeng.alt.oss.core.OssException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 本地文件系统 {@link OssTemplate} 实现。
 * <p>
 * 使用本地磁盘作为存储后端，适用于开发测试或小规模文件存储场景。
 * endpoint 格式：{@code file:///d:/data/oss}（Windows）或 {@code file:///data/oss}（Linux/Mac）。
 * <p>
 * 目录结构：
 * <pre>
 * {basePath}/
 * ├── {bucketName}/
 * │   ├── path/to/file1.jpg
 * │   ├── path/to/file2.pdf
 * │   └── ...
 * └── ...
 * </pre>
 * <p>
 * 注意：本地文件系统不支持预签名 URL，{@link #presignedGetUrl} 返回 {@code null}。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public class FileSystemOssTemplate implements OssTemplate {

    private static final Logger log = LoggerFactory.getLogger(FileSystemOssTemplate.class);

    /** 基础存储路径 */
    private final Path basePath;

    /** 默认桶名 */
    private final String bucketName;

    /** 基础路径前缀（可选） */
    private final String basePrefix;

    /** 是否启用桶策略（自动建桶） */
    private final boolean autoCreateBucket;

    /**
     * 构造本地文件系统模板。
     *
     * @param properties OSS 配置属性
     * @throws OssException 如果 endpoint 不是有效的 file:// URI
     */
    public FileSystemOssTemplate(OssProperties properties) {
        this.bucketName = properties.getBucketName() != null ? properties.getBucketName() : "default";
        this.basePrefix = properties.getBasePath() != null ? properties.getBasePath() : "";
        this.autoCreateBucket = properties.isAutoCreateBucket();

        // 解析 file:// 协议的路径
        String endpoint = properties.getEndpoint();
        if (endpoint == null || !endpoint.startsWith("file://")) {
            throw new OssException("FileSystemOssTemplate requires endpoint starting with 'file://', got: " + endpoint);
        }

        try {
            URI uri = new URI(endpoint);
            this.basePath = Paths.get(uri);
        } catch (URISyntaxException | FileSystemNotFoundException | IllegalArgumentException e) {
            throw new OssException("Invalid file URI: " + endpoint, e);
        }

        // 确保基础目录存在
        try {
            Files.createDirectories(this.basePath);
            log.info("FileSystem OSS initialized: basePath={}, bucket={}", this.basePath, this.bucketName);
        } catch (IOException e) {
            throw new OssException("Cannot create base directory: " + this.basePath, e);
        }

        if (this.autoCreateBucket) {
            ensureBucketExists(this.bucketName);
        }
    }

    // ==================== 上传 ====================

    @Override
    public OssFileInfo upload(InputStream inputStream, String fileName) {
        return upload(inputStream, fileName, null);
    }

    @Override
    public OssFileInfo upload(InputStream inputStream, String fileName, String contentType) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File upload success: bucket={}, path={}", bucketName, fullPath);
            return buildFileInfo(fullPath, contentType, targetFile);
        } catch (IOException e) {
            throw new OssException("File upload failed: " + fullPath, e);
        }
    }

    @Override
    public OssFileInfo upload(byte[] data, String fileName) {
        return upload(data, fileName, null);
    }

    @Override
    public OssFileInfo upload(byte[] data, String fileName, String contentType) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        try {
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, data);
            log.debug("File upload success: bucket={}, path={}", bucketName, fullPath);
            return buildFileInfo(fullPath, contentType, targetFile);
        } catch (IOException e) {
            throw new OssException("File upload failed: " + fullPath, e);
        }
    }

    @Override
    public OssFileInfo upload(File file, String fileName) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(file.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File upload success: bucket={}, path={}", bucketName, fullPath);
            OssFileInfo info = buildFileInfo(fullPath, null, targetFile);
            info.setSize(file.length());
            return info;
        } catch (IOException e) {
            throw new OssException("File upload failed: " + fullPath, e);
        }
    }

    // ==================== 下载 ====================

    @Override
    public InputStream download(String fileName) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        try {
            if (Files.notExists(targetFile)) {
                return null;
            }
            return Files.newInputStream(targetFile);
        } catch (IOException e) {
            throw new OssException("File download failed: " + fullPath, e);
        }
    }

    // ==================== 删除 ====================

    @Override
    public void delete(String fileName) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        try {
            Files.deleteIfExists(targetFile);
            log.debug("File delete success: bucket={}, path={}", bucketName, fullPath);
            // 清理空目录
            cleanEmptyParent(targetFile.getParent());
        } catch (IOException e) {
            throw new OssException("File delete failed: " + fullPath, e);
        }
    }

    @Override
    public void delete(List<String> fileNames) {
        for (String fileName : fileNames) {
            delete(fileName);
        }
    }

    // ==================== 查询 ====================

    @Override
    public boolean exists(String fileName) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        return Files.exists(targetFile);
    }

    @Override
    public OssFileInfo getFileInfo(String fileName) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        try {
            if (Files.notExists(targetFile)) {
                return null;
            }
            return buildFileInfo(fullPath, null, targetFile);
        } catch (IOException e) {
            throw new OssException("File getFileInfo failed: " + fullPath, e);
        }
    }

    @Override
    public String getUrl(String fileName) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        if (Files.notExists(targetFile)) {
            return null;
        }
        return targetFile.toUri().toString();
    }

    @Override
    public List<OssFileInfo> listFiles(String prefix) {
        String fullPrefix = buildFullPath(prefix);
        String searchPrefix = fullPrefix;
        Path searchDir = resolvePath(bucketName, searchPrefix);
        if (searchPrefix.isEmpty()) {
            searchDir = resolvePath(bucketName, "");
        }

        List<OssFileInfo> result = new ArrayList<>();
        if (Files.notExists(searchDir)) {
            return result;
        }

        final Path walkRoot = searchDir;
        try (Stream<Path> walk = Files.walk(walkRoot, Integer.MAX_VALUE)) {
            walk.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String relativePath = walkRoot.relativize(path).toString()
                                    .replace("\\", "/");
                            String fullPath = searchPrefix.isEmpty() ? relativePath
                                    : searchPrefix.endsWith("/") ? searchPrefix + relativePath
                                    : searchPrefix + "/" + relativePath;
                            OssFileInfo info = buildFileInfo(fullPath, null, path);
                            result.add(info);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            throw new OssException("File list failed for prefix: " + fullPrefix, e);
        }

        return result;
    }

    // ==================== 其他 ====================

    @Override
    public void copy(String sourceFileName, String targetFileName) {
        String sourceFullPath = buildFullPath(sourceFileName);
        String targetFullPath = buildFullPath(targetFileName);
        Path sourceFile = resolvePath(bucketName, sourceFullPath);
        Path targetFile = resolvePath(bucketName, targetFullPath);
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File copy success: {} -> {}", sourceFullPath, targetFullPath);
        } catch (IOException e) {
            throw new OssException("File copy failed: " + sourceFullPath + " -> " + targetFullPath, e);
        }
    }

    @Override
    public void move(String sourceFileName, String targetFileName) {
        String sourceFullPath = buildFullPath(sourceFileName);
        String targetFullPath = buildFullPath(targetFileName);
        Path sourceFile = resolvePath(bucketName, sourceFullPath);
        Path targetFile = resolvePath(bucketName, targetFullPath);
        try {
            Files.createDirectories(targetFile.getParent());
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File move success: {} -> {}", sourceFullPath, targetFullPath);
            cleanEmptyParent(sourceFile.getParent());
        } catch (IOException e) {
            throw new OssException("File move failed: " + sourceFullPath + " -> " + targetFullPath, e);
        }
    }

    @Override
    public String presignedGetUrl(String fileName, int expiration) {
        // 本地文件系统不支持预签名 URL
        log.warn("presignedGetUrl is not supported by FileSystemOssTemplate, returning direct file URI");
        return getUrl(fileName);
    }

    // ==================== 桶感知操作 ====================

    @Override
    public OssFileInfo upload(String bucketName, InputStream inputStream, String fileName) {
        return upload(bucketName, inputStream, fileName, null);
    }

    @Override
    public OssFileInfo upload(String bucketName, InputStream inputStream, String fileName, String contentType) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File upload (bucket-aware) success: bucket={}, path={}", bucketName, fullPath);
            return buildFileInfo(bucketName, fullPath, contentType, targetFile);
        } catch (IOException e) {
            throw new OssException("File upload failed to bucket " + bucketName + ": " + fullPath, e);
        }
    }

    @Override
    public OssFileInfo upload(String bucketName, byte[] data, String fileName) {
        return upload(bucketName, data, fileName, null);
    }

    @Override
    public OssFileInfo upload(String bucketName, byte[] data, String fileName, String contentType) {
        String fullPath = buildFullPath(fileName);
        Path targetFile = resolvePath(bucketName, fullPath);
        try {
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, data);
            log.debug("File upload (bucket-aware) success: bucket={}, path={}", bucketName, fullPath);
            return buildFileInfo(bucketName, fullPath, contentType, targetFile);
        } catch (IOException e) {
            throw new OssException("File upload failed to bucket " + bucketName + ": " + fullPath, e);
        }
    }

    @Override
    public boolean exists(String bucketName, String fileName) {
        Path targetFile = resolvePath(bucketName, buildFullPath(fileName));
        return Files.exists(targetFile);
    }

    @Override
    public void delete(String bucketName, String fileName) {
        Path targetFile = resolvePath(bucketName, buildFullPath(fileName));
        try {
            Files.deleteIfExists(targetFile);
            cleanEmptyParent(targetFile.getParent());
        } catch (IOException e) {
            throw new OssException("File delete failed from bucket " + bucketName + ": " + fileName, e);
        }
    }

    @Override
    public void ensureBucketExists(String bucketName) {
        Path bucketDir = basePath.resolve(sanitizeBucketName(bucketName));
        try {
            Files.createDirectories(bucketDir);
            log.debug("Local bucket directory ensured: {}", bucketDir);
        } catch (IOException e) {
            throw new OssException("Cannot create bucket directory: " + bucketDir, e);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 拼接完整路径（basePrefix + fileName）
     */
    private String buildFullPath(String fileName) {
        if (!StringUtils.hasText(basePrefix)) {
            return fileName;
        }
        String normalizedBase = basePrefix.endsWith("/") ? basePrefix : basePrefix + "/";
        return normalizedBase + fileName;
    }

    /**
     * 将逻辑路径解析为文件系统路径。
     */
    private Path resolvePath(String bucket, String fullPath) {
        String safeBucket = sanitizeBucketName(bucket);
        String normalizedPath = fullPath != null ? fullPath.replace("\\", "/") : "";
        // 路径分隔符统一使用系统分隔符
        String osPath = normalizedPath.replace("/", File.separator);
        return basePath.resolve(safeBucket).resolve(osPath).normalize();
    }

    /**
     * 构建 OssFileInfo（使用配置的默认桶）
     */
    private OssFileInfo buildFileInfo(String fullPath, String contentType, Path filePath) throws IOException {
        return buildFileInfo(bucketName, fullPath, contentType, filePath);
    }

    /**
     * 构建 OssFileInfo（指定桶名）
     */
    private OssFileInfo buildFileInfo(String bucketName, String fullPath, String contentType, Path filePath) throws IOException {
        OssFileInfo info = new OssFileInfo();
        info.setFileName(fullPath);
        info.setBucketName(bucketName);
        info.setSize(Files.size(filePath));
        info.setContentType(contentType);
        info.setUrl(filePath.toUri().toString());
        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            if (attrs.lastModifiedTime() != null) {
                info.setLastModified(LocalDateTime.ofInstant(
                        attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));
            }
        } catch (Exception ignored) {
        }
        // 使用文件名作为 ETag
        info.setEtag(filePath.getFileName().toString());
        return info;
    }

    /**
     * 安全化桶名：替换不安全的文件名字符。
     */
    private String sanitizeBucketName(String bucket) {
        if (bucket == null) return "default";
        return bucket.replaceAll("[<>:\"/\\\\|?*]", "_");
    }

    /**
     * 清理空父目录（递归）。
     */
    private void cleanEmptyParent(Path dir) {
        try {
            if (dir != null && Files.isDirectory(dir) && Files.list(dir).findAny().isEmpty()) {
                Files.delete(dir);
                log.debug("Removed empty directory: {}", dir);
                // 递归清理祖父目录
                cleanEmptyParent(dir.getParent());
            }
        } catch (IOException ignored) {
        }
    }
}
