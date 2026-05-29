package com.github.zeng.alt.i18n.rest;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.i18n.core.I18nMessageService;
import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import jakarta.servlet.ServletException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.List;

/**
 * 国际化消息 MVC Handler — 处理函数式路由的具体业务逻辑
 *
 * @author zengJiaJun
 * @since 2026年05月29日
 * @version 1.0
 */
public class I18nMvcHandler {

    private final I18nMessageService i18nMessageService;

    public I18nMvcHandler(I18nMessageService i18nMessageService) {
        this.i18nMessageService = i18nMessageService;
    }

    public ServerResponse findAll(ServerRequest request) {
        List<I18nMessageDO> list = i18nMessageService.findAll();
        return ServerResponse.ok().body(RestResponse.success(list));
    }

    public ServerResponse findByCodeAndLocale(ServerRequest request) {
        String code = request.pathVariable("code");
        String locale = request.pathVariable("locale");
        return i18nMessageService.findByCodeAndLocale(code, locale)
                .map(msg -> ServerResponse.ok().body(RestResponse.success(msg)))
                .getOrElse(ServerResponse.notFound().build());
    }

    public ServerResponse findByCode(ServerRequest request) {
        String code = request.pathVariable("code");
        List<I18nMessageDO> list = i18nMessageService.findByCode(code);
        return ServerResponse.ok().body(RestResponse.success(list));
    }

    public ServerResponse findByLocale(ServerRequest request) {
        String locale = request.pathVariable("locale");
        List<I18nMessageDO> list = i18nMessageService.findByLocale(locale);
        return ServerResponse.ok().body(RestResponse.success(list));
    }

    public ServerResponse create(ServerRequest request) throws IOException, ServletException {
        I18nMessageDO message = request.body(I18nMessageDO.class);
        if (message.getId() != null) {
            return ServerResponse.badRequest().body(RestResponse.fail("新增时不能携带 ID"));
        }
        I18nMessageDO saved = i18nMessageService.save(message);
        return ServerResponse.ok().body(RestResponse.success(saved));
    }

    public ServerResponse update(ServerRequest request) throws IOException, ServletException {
        I18nMessageDO message = request.body(I18nMessageDO.class);
        return i18nMessageService.findByCodeAndLocale(message.getCode(), message.getLocale())
                .map(existing -> {
                    existing.setMessage(message.getMessage());
                    existing.setModule(message.getModule());
                    I18nMessageDO saved = i18nMessageService.save(existing);
                    return ServerResponse.ok().body(RestResponse.success(saved));
                })
                .getOrElse(ServerResponse.notFound().build());
    }

    public ServerResponse deleteById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        i18nMessageService.deleteById(id);
        return ServerResponse.ok().body(RestResponse.success());
    }

    public ServerResponse deleteByCodeAndLocale(ServerRequest request) {
        String code = request.pathVariable("code");
        String locale = request.pathVariable("locale");
        i18nMessageService.deleteByCodeAndLocale(code, locale);
        return ServerResponse.ok().body(RestResponse.success());
    }
}