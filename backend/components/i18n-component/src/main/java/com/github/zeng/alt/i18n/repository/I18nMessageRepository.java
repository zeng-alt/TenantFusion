package com.github.zeng.alt.i18n.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import io.vavr.control.Option;

import java.util.List;

/**
 * 国际化消息 Repository
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
public interface I18nMessageRepository extends BaseRepository<I18nMessageDO, Long> {

    Option<I18nMessageDO> findByCodeAndLocale(String code, String locale);

    List<I18nMessageDO> findByLocale(String locale);

    List<I18nMessageDO> findByCode(String code);

    void deleteAll();
}