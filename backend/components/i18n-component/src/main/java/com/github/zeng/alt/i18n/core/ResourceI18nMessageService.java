package com.github.zeng.alt.i18n.core;

import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import io.vavr.control.Option;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 资源文件模式下的国际化消息服务实现
 * <p>
 * 基于 {@link MessageSource} 和 {@link ResourceBundle} 从属性文件中读取消息，
 * 仅支持查询操作，不支持运行时新增、修改、删除（{@link #save(I18nMessageDO)}、
 * {@link #deleteById(Long)}、{@link #deleteByCodeAndLocale(String, String)}
 * 会抛出 {@link UnsupportedOperationException}）。
 * </p>
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
public class ResourceI18nMessageService implements I18nMessageService {

    private final MessageSource messageSource;
    private final String basename;

    public ResourceI18nMessageService(MessageSource messageSource, String basename) {
        this.messageSource = messageSource;
        this.basename = basename;
    }

    @Override
    public Option<I18nMessageDO> findByCodeAndLocale(String code, String locale) {
        try {
            Locale localeObj = Locale.forLanguageTag(locale.replace('_', '-'));
            String message = messageSource.getMessage(code, null, localeObj);
            I18nMessageDO msg = new I18nMessageDO();
            msg.setCode(code);
            msg.setLocale(locale);
            msg.setMessage(message);
            return Option.some(msg);
        } catch (NoSuchMessageException e) {
            return Option.none();
        }
    }

    @Override
    public List<I18nMessageDO> findByCode(String code) {
        // 资源文件模式下无法枚举所有区域，只返回默认区域的结果
        Option<I18nMessageDO> result = findByCodeAndLocale(code,
                Locale.getDefault().toString().replace('-', '_'));
        return result.isDefined() ? List.of(result.get()) : Collections.emptyList();
    }

    @Override
    public List<I18nMessageDO> findByLocale(String locale) {
        try {
            Locale localeObj = Locale.forLanguageTag(locale.replace('_', '-'));
            ResourceBundle bundle = ResourceBundle.getBundle(basename, localeObj);
            List<I18nMessageDO> result = new ArrayList<>();
            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String code = keys.nextElement();
                I18nMessageDO msg = new I18nMessageDO();
                msg.setCode(code);
                msg.setLocale(locale);
                msg.setMessage(bundle.getString(code));
                result.add(msg);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<I18nMessageDO> findAll() {
        return findByLocale(Locale.getDefault().toString().replace('-', '_'));
    }

    @Override
    public I18nMessageDO save(I18nMessageDO message) {
        throw new UnsupportedOperationException(
                "文件模式不支持运行时新增/修改消息，请直接编辑 " + basename + ".properties 文件");
    }

    @Override
    public void deleteById(Long id) {
        throw new UnsupportedOperationException(
                "文件模式不支持运行时删除消息，请直接编辑 " + basename + ".properties 文件");
    }

    @Override
    public void deleteByCodeAndLocale(String code, String locale) {
        throw new UnsupportedOperationException(
                "文件模式不支持运行时删除消息，请直接编辑 " + basename + ".properties 文件");
    }
}