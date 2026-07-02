package com.github.zeng.alt.oss.core;

import com.github.zeng.alt.oss.OssFileInfo;
import com.github.zeng.alt.oss.OssTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 可热替换的 {@link OssTemplate} 装饰器。
 * <p>
 * 包装一个真实的 {@link OssTemplate} 实现，支持在运行时原子性地切换底层委托。
 * 切换后，正在使用旧委托的请求不受影响，新请求自动使用新委托。
 * 适用于动态刷新 OSS 连接而不中断正在运行的文件操作。
 * <p>
 * 线程安全：通过 {@link AtomicReference} 保证委托的读写可见性和原子替换。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
public class RefreshableOssTemplate implements OssTemplate {

    private static final Logger log = LoggerFactory.getLogger(RefreshableOssTemplate.class);

    private final AtomicReference<OssTemplate> delegateRef;

    /**
     * 创建可热替换的模板。
     *
     * @param initialDelegate 初始委托实现
     */
    public RefreshableOssTemplate(OssTemplate initialDelegate) {
        this.delegateRef = new AtomicReference<>(initialDelegate);
    }

    /**
     * 获取当前委托。
     */
    public OssTemplate getDelegate() {
        return delegateRef.get();
    }

    /**
     * 原子性地切换委托。
     *
     * @param newDelegate 新委托
     * @return 旧的委托，调用者可自行关闭
     */
    public OssTemplate swapDelegate(OssTemplate newDelegate) {
        OssTemplate old = delegateRef.getAndSet(newDelegate);
        log.debug("OSS template delegate swapped: {} -> {}",
                old.getClass().getSimpleName(), newDelegate.getClass().getSimpleName());
        return old;
    }

    @Override
    public OssFileInfo upload(InputStream inputStream, String fileName) {
        return delegateRef.get().upload(inputStream, fileName);
    }

    @Override
    public OssFileInfo upload(InputStream inputStream, String fileName, String contentType) {
        return delegateRef.get().upload(inputStream, fileName, contentType);
    }

    @Override
    public OssFileInfo upload(byte[] data, String fileName) {
        return delegateRef.get().upload(data, fileName);
    }

    @Override
    public OssFileInfo upload(byte[] data, String fileName, String contentType) {
        return delegateRef.get().upload(data, fileName, contentType);
    }

    @Override
    public OssFileInfo upload(File file, String fileName) {
        return delegateRef.get().upload(file, fileName);
    }

    @Override
    public InputStream download(String fileName) {
        return delegateRef.get().download(fileName);
    }

    @Override
    public void delete(String fileName) {
        delegateRef.get().delete(fileName);
    }

    @Override
    public void delete(List<String> fileNames) {
        delegateRef.get().delete(fileNames);
    }

    @Override
    public boolean exists(String fileName) {
        return delegateRef.get().exists(fileName);
    }

    @Override
    public OssFileInfo getFileInfo(String fileName) {
        return delegateRef.get().getFileInfo(fileName);
    }

    @Override
    public String getUrl(String fileName) {
        return delegateRef.get().getUrl(fileName);
    }

    @Override
    public List<OssFileInfo> listFiles(String prefix) {
        return delegateRef.get().listFiles(prefix);
    }

    @Override
    public void copy(String sourceFileName, String targetFileName) {
        delegateRef.get().copy(sourceFileName, targetFileName);
    }

    @Override
    public void move(String sourceFileName, String targetFileName) {
        delegateRef.get().move(sourceFileName, targetFileName);
    }

    @Override
    public String presignedGetUrl(String fileName, int expiration) {
        return delegateRef.get().presignedGetUrl(fileName, expiration);
    }

    // ==================== 桶感知操作 ====================

    @Override
    public OssFileInfo upload(String bucketName, InputStream inputStream, String fileName) {
        return delegateRef.get().upload(bucketName, inputStream, fileName);
    }

    @Override
    public OssFileInfo upload(String bucketName, InputStream inputStream, String fileName, String contentType) {
        return delegateRef.get().upload(bucketName, inputStream, fileName, contentType);
    }

    @Override
    public OssFileInfo upload(String bucketName, byte[] data, String fileName) {
        return delegateRef.get().upload(bucketName, data, fileName);
    }

    @Override
    public OssFileInfo upload(String bucketName, byte[] data, String fileName, String contentType) {
        return delegateRef.get().upload(bucketName, data, fileName, contentType);
    }

    @Override
    public boolean exists(String bucketName, String fileName) {
        return delegateRef.get().exists(bucketName, fileName);
    }

    @Override
    public void delete(String bucketName, String fileName) {
        delegateRef.get().delete(bucketName, fileName);
    }

    @Override
    public void ensureBucketExists(String bucketName) {
        delegateRef.get().ensureBucketExists(bucketName);
    }
}
