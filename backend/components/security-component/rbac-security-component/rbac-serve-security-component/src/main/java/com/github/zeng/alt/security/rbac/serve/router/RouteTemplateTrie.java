package com.github.zeng.alt.security.rbac.serve.router;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 路由模板前缀树（Trie），支持路径变量匹配。
 *
 * <p>用于将实际请求路径（如 {@code /users/123}）匹配到路由模板（如 {@code /users/{id}}）。
 * 线程安全，内部使用 {@link ReadWriteLock} 控制并发。</p>
 *
 * <p>匹配规则：</p>
 * <ul>
 *   <li>优先匹配静态路径段</li>
 *   <li>其次匹配变量路径段（{@code {name}} 格式）</li>
 * </ul>
 */
@Slf4j
public class RouteTemplateTrie {

    private final TrieNode root = new TrieNode();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    static class TrieNode {
        Map<String, TrieNode> children = new HashMap<>();
        Map<String, String> templates = new HashMap<>();
        boolean isVariable = false;
    }

    private String normalizePart(String part) {
        return (part.startsWith("{") && part.endsWith("}")) ? "{}" : part;
    }

    /**
     * 插入一个路由模板到前缀树（不限定 HTTP 方法）。
     *
     * @param template 路由模板，如 {@code /api/users/{id}/details}
     */
    public void insert(String template) {
        doInsert("*", template);
    }

    /**
     * 插入一个路由模板到前缀树，关联指定的 HTTP 方法。
     *
     * @param method   HTTP 方法（{@code GET}、{@code POST} 等），或 {@code null} 表示不限定方法
     * @param template 路由模板，如 {@code /api/users/{id}/details}
     */
    public void insert(String method, String template) {
        doInsert(method, template);
    }

    private void doInsert(String method, String template) {
        lock.writeLock().lock();
        try {
            String[] parts = template.split("/");
            TrieNode node = root;
            for (String part : parts) {
                if (part.isEmpty()) continue;

                String key = normalizePart(part);
                node.children.putIfAbsent(key, new TrieNode());
                node = node.children.get(key);
                node.isVariable = "{}".equals(key);
            }
            node.templates.put(method, template);
            log.trace("Inserted route template [{}] for method [{}]", template, method);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 将实际请求路径匹配到已注册的路由模板（不限定 HTTP 方法）。
     *
     * @param actualPath 实际请求路径，如 {@code /api/users/123/details}
     * @return 匹配到的路由模板，如 {@code /api/users/{id}/details}；无匹配时返回 {@code null}
     */
    public String match(String actualPath) {
        return match(null, actualPath);
    }

    /**
     * 将实际请求路径和 HTTP 方法匹配到已注册的路由模板。
     * <p>优先匹配方法相关的模板；未命中时回退到不限定方法的模板。</p>
     *
     * @param method     HTTP 方法（{@code GET}、{@code POST} 等），为 {@code null} 时只匹配不限定方法的模板
     * @param actualPath 实际请求路径，如 {@code /api/users/123/details}
     * @return 匹配到的路由模板；无匹配时返回 {@code null}
     */
    public String match(String method, String actualPath) {
        lock.readLock().lock();
        try {
            String[] parts = this.tokenizePath(actualPath);
            TrieNode node = matchRecursive(parts, 0, root);
            if (node == null) return null;

            String result = null;
            if (method != null) {
                result = node.templates.get(method);
            }
            if (result == null) {
                result = node.templates.get("*");
            }
            log.trace("Path [{}] method [{}] matched to template [{}]", actualPath, method, result);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    private String[] tokenizePath(String path) {
        return Arrays.stream(path.split("/"))
                .filter(part -> !part.isEmpty())
                .toArray(String[]::new);
    }

    private TrieNode matchRecursive(String[] parts, int index, TrieNode node) {
        if (index == parts.length) {
            return node.templates.isEmpty() ? null : node;
        }

        String part = parts[index];

        TrieNode exactNode = node.children.get(part);
        if (exactNode != null) {
            TrieNode result = matchRecursive(parts, index + 1, exactNode);
            if (result != null) return result;
        }

        TrieNode variableNode = node.children.get("{}");
        if (variableNode != null) {
            TrieNode result = matchRecursive(parts, index + 1, variableNode);
            if (result != null) return result;
        }

        return null;
    }

    /**
     * 获取所有已注册的路由模板。
     */
    public List<String> getAllTemplates() {
        List<String> results = new ArrayList<>();
        lock.readLock().lock();
        try {
            collectTemplates(root, results);
        } finally {
            lock.readLock().unlock();
        }
        return results;
    }

    private void collectTemplates(TrieNode node, List<String> results) {
        if (!node.templates.isEmpty()) {
            results.addAll(node.templates.values());
        }
        for (TrieNode child : node.children.values()) {
            collectTemplates(child, results);
        }
    }

    /**
     * 删除以某个路径为前缀的所有模板路径。
     * <p>用于当某业务服务的路由整体刷新时，先清理旧路由再插入新路由。</p>
     *
     * @param prefixPath 路径前缀，如 {@code /api/users}
     */
    public void deleteSubtree(String prefixPath) {
        lock.writeLock().lock();
        try {
            log.debug("Deleting route subtree for prefix: {}", prefixPath);
            String[] parts = tokenizePath(prefixPath);
            deleteRecursive(root, parts, 0);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean deleteRecursive(TrieNode node, String[] parts, int index) {
        if (index == parts.length) {
            node.children.clear();
            node.templates.clear();
            return true;
        }

        String key = normalizePart(parts[index]);
        TrieNode child = node.children.get(key);
        if (child != null) {
            boolean deleted = deleteRecursive(child, parts, index + 1);
            if (deleted) {
                if (child.children.isEmpty() && child.templates.isEmpty()) {
                    node.children.remove(key);
                }
            }
        }
        return false;
    }
}
