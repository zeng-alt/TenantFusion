package com.github.zeng.alt.i18n.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.i18n.entity.SystemI18nMessageDO;
import io.vavr.control.Option;

import java.util.List;

/**
 * 国际化消息 Repository
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
public interface I18nMessageRepository extends BaseRepository<SystemI18nMessageDO, Long> {

    Option<SystemI18nMessageDO> findByCodeAndLocale(String code, String locale);

    List<SystemI18nMessageDO> findByLocale(String locale);

    List<SystemI18nMessageDO> findByCode(String code);

    void deleteAll();
}