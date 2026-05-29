package com.github.zeng.alt.i18n.core;

import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import com.github.zeng.alt.i18n.repository.I18nMessageRepository;
import io.vavr.control.Option;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 数据库模式下的国际化消息服务实现
 * <p>
 * 基于 JPA 操作数据库，写入后自动清除 {@link DatabaseMessageSource} 的本地缓存，
 * 使修改立即生效。
 * </p>
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
public class DatabaseI18nMessageService implements I18nMessageService {

    private final I18nMessageRepository i18nMessageRepository;
    private final MessageSource messageSource;

    public DatabaseI18nMessageService(I18nMessageRepository i18nMessageRepository, MessageSource messageSource) {
        this.i18nMessageRepository = i18nMessageRepository;
        this.messageSource = messageSource;
    }

    @Override
    public Option<I18nMessageDO> findByCodeAndLocale(String code, String locale) {
        return i18nMessageRepository.findByCodeAndLocale(code, locale);
    }

    @Override
    public List<I18nMessageDO> findByCode(String code) {
        return i18nMessageRepository.findByCode(code);
    }

    @Override
    public List<I18nMessageDO> findByLocale(String locale) {
        return i18nMessageRepository.findByLocale(locale);
    }

    @Override
    public List<I18nMessageDO> findAll() {
        return i18nMessageRepository.findAll();
    }

    @Override
    @Transactional
    public I18nMessageDO save(I18nMessageDO message) {
        I18nMessageDO saved = i18nMessageRepository.save(message);
        evictCache(saved.getCode(), saved.getLocale());
        return saved;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        i18nMessageRepository.findById(id)
                .peek(existing -> {
                    i18nMessageRepository.deleteById(id);
                    evictCache(existing.getCode(), existing.getLocale());
                });
    }

    @Override
    @Transactional
    public void deleteByCodeAndLocale(String code, String locale) {
        i18nMessageRepository.findByCodeAndLocale(code, locale)
                .peek(msg -> {
                    i18nMessageRepository.deleteById(msg.getId());
                    evictCache(code, locale);
                });
    }

    private void evictCache(String code, String locale) {
        if (messageSource instanceof DatabaseMessageSource dbSource) {
            dbSource.evictCache(code, Locale.forLanguageTag(locale.replace('_', '-')));
        }
    }
}