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
        String fullTemplate = null;
        boolean isVariable = false;
    }

    private String normalizePart(String part) {
        return (part.startsWith("{") && part.endsWith("}")) ? "{}" : part;
    }

    /**
     * 插入一个路由模板到前缀树。
     *
     * @param template 路由模板，如 {@code /api/users/{id}/details}
     */
    public void insert(String template) {
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
            node.fullTemplate = template;
            log.trace("Inserted route template: {}", template);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 将实际请求路径匹配到已注册的路由模板。
     *
     * @param actualPath 实际请求路径，如 {@code /api/users/123/details}
     * @return 匹配到的路由模板，如 {@code /api/users/{id}/details}；无匹配时返回 {@code null}
     */
    public String match(String actualPath) {
        lock.readLock().lock();
        try {
            String[] parts = this.tokenizePath(actualPath);
            String result = matchRecursive(parts, 0, root);
            log.trace("Path [{}] matched to template [{}]", actualPath, result);
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

    private String matchRecursive(String[] parts, int index, TrieNode node) {
        if (index == parts.length) {
            return node.fullTemplate;
        }

        String part = parts[index];

        TrieNode exactNode = node.children.get(part);
        if (exactNode != null) {
            String result = matchRecursive(parts, index + 1, exactNode);
            if (result != null) return result;
        }

        TrieNode variableNode = node.children.get("{}");
        if (variableNode != null) {
            String result = matchRecursive(parts, index + 1, variableNode);
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
        if (node.fullTemplate != null) {
            results.add(node.fullTemplate);
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
            node.fullTemplate = null;
            return true;
        }

        String key = normalizePart(parts[index]);
        TrieNode child = node.children.get(key);
        if (child != null) {
            boolean deleted = deleteRecursive(child, parts, index + 1);
            if (deleted) {
                if (child.children.isEmpty() && child.fullTemplate == null) {
                    node.children.remove(key);
                }
            }
        }
        return false;
    }
}
