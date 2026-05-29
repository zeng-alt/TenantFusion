package com.github.zeng.alt.i18n.core;

import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import com.github.zeng.alt.i18n.repository.I18nMessageRepository;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractMessageSource;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于数据库的 MessageSource 实现
 * <p>
 * 从 {@link I18nMessageRepository} 中查询国际化消息，
 * 并缓存到本地内存中以提升性能。
 * 无缓存时直接查询数据库。
 * </p>
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
public class DatabaseMessageSource extends AbstractMessageSource {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMessageSource.class);

    private final I18nMessageRepository i18nMessageRepository;

    /**
     * 本地消息缓存：code#locale -> message
     */
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    public DatabaseMessageSource(I18nMessageRepository i18nMessageRepository) {
        this.i18nMessageRepository = i18nMessageRepository;
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String message = resolveMessage(code, locale);
        if (message == null) {
            // 尝试使用不带国家的语言（如 zh_CN -> zh）
            if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
                Locale languageLocale = Locale.forLanguageTag(locale.getLanguage());
                message = resolveMessage(code, languageLocale);
            }
        }
        if (message == null) {
            log.debug("i18n message not found: code={}, locale={}", code, locale);
            return null;
        }
        return createMessageFormat(message, locale);
    }

    /**
     * 从缓存或数据库查询消息
     *
     * @param code   消息编码
     * @param locale 区域
     * @return 消息文本，未找到返回 null
     */
    private String resolveMessage(String code, Locale locale) {
        String cacheKey = buildCacheKey(code, locale);
        // 1. 尝试从缓存获取
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. 从数据库查询
        Option<I18nMessageDO> result = i18nMessageRepository.findByCodeAndLocale(code, locale.toString());
        if (result.isDefined()) {
            String message = result.get().getMessage();
            cache.put(cacheKey, message);
            return message;
        }
        return null;
    }

    /**
     * 清除所有缓存
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * 清除指定消息的缓存
     *
     * @param code   消息编码
     * @param locale 区域
     */
    public void evictCache(String code, Locale locale) {
        cache.remove(buildCacheKey(code, locale));
    }

    private static String buildCacheKey(String code, Locale locale) {
        return code + "#" + locale.toString();
    }
}
