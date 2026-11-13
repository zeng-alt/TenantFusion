package com.github.zeng.alt.security.rbac.serve.router;


import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由模板管理器。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>维护 {@link RouteTemplateTrie} 前缀树，用于路径模板匹配</li>
 *   <li>由 RouteTemplateEventListener 接收远程路由注册事件（微服务模式）</li>
 *   <li>提供 {@link #addRouteTemplate(String, List)} 方法供本地直接注册（单体模式）</li>
 * </ul>
 *
 * <p>无消息队列时，单体模式的 {@code DirectRouteTemplateRegistrar} 直接调用 {@code addRouteTemplate()} 注册路由。</p>
 */
@Slf4j
public class RouteTemplateManager {

    private static final Pattern METHOD_PREFIX = Pattern.compile("^([A-Z]+):(/.*)$");

    private final RouteTemplateTrie trie = new RouteTemplateTrie();
    /**
     * 注册或刷新指定上下文路径下的所有路由模板。
     * <p>先删除旧模板，再逐条插入新模板。</p>
     *
     * @param contextPath 上下文路径，用于删除旧路由
     * @param templates   路由模板列表
     */
    public void addRouteTemplate(String contextPath, List<String> templates) {
        log.debug("Adding {} route templates for contextPath '{}'", templates.size(), contextPath);
        trie.deleteSubtree(contextPath);
        for (String entry : templates) {
            Matcher matcher = METHOD_PREFIX.matcher(entry);
            if (matcher.matches()) {
                trie.insert(matcher.group(1), matcher.group(2));
            } else {
                trie.insert(entry);
            }
        }
        log.info("Registered {} route templates for contextPath '{}'", templates.size(), contextPath);
    }

    /**
     * 将实际请求路径匹配到路由模板（不限定 HTTP 方法）。
     *
     * @param path 实际请求路径
     * @return 匹配到的路由模板，无匹配时返回原路径
     */
    public String match(String path) {
        return match(null, path);
    }

    /**
     * 将实际请求路径和 HTTP 方法匹配到路由模板。
     *
     * @param method HTTP 方法（{@code GET}、{@code POST} 等），为 {@code null} 时只匹配不限定方法的模板
     * @param path   实际请求路径
     * @return 匹配到的路由模板，无匹配时返回原路径
     */
    public String match(String method, String path) {
        String match = trie.match(method, path);
        return StringUtils.hasText(match) ? match : path;
    }

}
