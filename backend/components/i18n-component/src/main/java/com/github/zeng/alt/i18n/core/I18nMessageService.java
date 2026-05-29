package com.github.zeng.alt.i18n.core;

import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import io.vavr.control.Option;

import java.util.List;

/**
 * 国际化消息服务接口
 * <p>
 * 定义对国际化消息的增删改查操作。
 * 根据 {@link com.github.zeng.alt.i18n.config.I18nProperties#mode} 的不同，
 * 有两种实现：
 * <ul>
 *   <li>{@code database} 模式 → {@link DatabaseI18nMessageService}，基于 JPA 操作数据库</li>
 *   <li>{@code file} 模式 → {@link ResourceI18nMessageService}，基于资源文件读取，不支持运行时写操作</li>
 * </ul>
 * </p>
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
public interface I18nMessageService {

    /**
     * 根据编码和区域查询消息
     */
    Option<I18nMessageDO> findByCodeAndLocale(String code, String locale);

    /**
     * 根据编码查询所有区域的消息
     */
    List<I18nMessageDO> findByCode(String code);

    /**
     * 根据区域查询所有消息
     */
    List<I18nMessageDO> findByLocale(String locale);

    /**
     * 查询所有消息
     */
    List<I18nMessageDO> findAll();

    /**
     * 保存或更新消息
     */
    I18nMessageDO save(I18nMessageDO message);

    /**
     * 根据 ID 删除消息
     */
    void deleteById(Long id);

    /**
     * 根据编码和区域删除消息
     */
    void deleteByCodeAndLocale(String code, String locale);
}