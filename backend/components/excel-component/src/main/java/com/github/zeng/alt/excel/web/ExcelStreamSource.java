package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.read.ExcelReadSpec;

/**
 * 一次「懒打开」的读取来源。
 * <p>
 * 响应式导入是懒执行的：订阅时才该真正准备数据源（把上传内容落到临时文件）并开始
 * 解析，终结（完成、出错、取消）时释放资源。把这两步抽成本接口，
 * {@link ExcelReactiveSupport} 就不必知道上传文件在 Servlet 栈是
 * {@code MultipartFile}、在 WebFlux 栈是 {@code FilePart}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface ExcelStreamSource {

    /**
     * 准备数据源并返回读取链。订阅时调用。
     *
     * @return 已绑定数据源的读取链
     * @throws Exception 准备失败（落盘失败等）
     */
    ExcelReadSpec<?> open() throws Exception;

    /**
     * 释放本次读取占用的资源（删除临时文件）。流终结时调用，实现不应抛异常。
     */
    void close();
}
