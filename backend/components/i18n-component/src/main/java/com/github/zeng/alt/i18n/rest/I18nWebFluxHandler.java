package com.github.zeng.alt.i18n.rest;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.i18n.core.I18nMessageService;
import com.github.zeng.alt.i18n.entity.I18nMessageDO;
import io.vavr.control.Option;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public class I18nWebFluxHandler {

    private final I18nMessageService i18nMessageService;

    public I18nWebFluxHandler(I18nMessageService i18nMessageService) {
        this.i18nMessageService = i18nMessageService;
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        List<I18nMessageDO> list = i18nMessageService.findAll();
        return ServerResponse.ok().bodyValue(RestResponse.success(list));
    }

    public Mono<ServerResponse> findByCodeAndLocale(ServerRequest request) {
        String code = request.pathVariable("code");
        String locale = request.pathVariable("locale");
        Option<I18nMessageDO> result = i18nMessageService.findByCodeAndLocale(code, locale);
        if (result.isDefined()) {
            return ServerResponse.ok().bodyValue(RestResponse.success(result.get()));
        }
        return ServerResponse.notFound().build();
    }

    public Mono<ServerResponse> findByCode(ServerRequest request) {
        String code = request.pathVariable("code");
        List<I18nMessageDO> list = i18nMessageService.findByCode(code);
        return ServerResponse.ok().bodyValue(RestResponse.success(list));
    }

    public Mono<ServerResponse> findByLocale(ServerRequest request) {
        String locale = request.pathVariable("locale");
        List<I18nMessageDO> list = i18nMessageService.findByLocale(locale);
        return ServerResponse.ok().bodyValue(RestResponse.success(list));
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(I18nMessageDO.class)
                .flatMap(message -> {
                    if (message.getId() != null) {
                        return ServerResponse.badRequest()
                                .bodyValue(RestResponse.fail("新增时不能携带 ID"));
                    }
                    I18nMessageDO saved = i18nMessageService.save(message);
                    return ServerResponse.ok().bodyValue(RestResponse.success(saved));
                });
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        return request.bodyToMono(I18nMessageDO.class)
                .flatMap(message -> {
                    String code = message.getCode();
                    String locale = message.getLocale();
                    Option<I18nMessageDO> existingOpt = i18nMessageService
                            .findByCodeAndLocale(code, locale);
                    if (existingOpt.isEmpty()) {
                        return ServerResponse.notFound().build();
                    }
                    I18nMessageDO existing = existingOpt.get();
                    existing.setMessage(message.getMessage());
                    existing.setModule(message.getModule());
                    I18nMessageDO saved = i18nMessageService.save(existing);
                    return ServerResponse.ok().bodyValue(RestResponse.success(saved));
                });
    }

    public Mono<ServerResponse> deleteById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        i18nMessageService.deleteById(id);
        return ServerResponse.ok().bodyValue(RestResponse.success());
    }

    public Mono<ServerResponse> deleteByCodeAndLocale(ServerRequest request) {
        String code = request.pathVariable("code");
        String locale = request.pathVariable("locale");
        i18nMessageService.deleteByCodeAndLocale(code, locale);
        return ServerResponse.ok().bodyValue(RestResponse.success());
    }
}