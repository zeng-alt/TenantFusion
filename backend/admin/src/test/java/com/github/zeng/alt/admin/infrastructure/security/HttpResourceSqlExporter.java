package com.github.zeng.alt.admin.infrastructure.security;

import com.github.zeng.alt.admin.AdminApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest(
        classes = {AdminApplication.class, HttpResourceSqlExporter.AuditConfig.class},
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.liquibase.enabled=false",
                "spring.jpa.show-sql=false",
                "spring.datasource.url=jdbc:h2:mem:sql_exporter;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                // RBAC 必须开启：UserServiceImpl 依赖 RbacResourceService，
                // 原先设为 false 会让 RbacAutoConfiguration 不生效，上下文直接起不来
                "security.context.enabled-access=true",
                // 本工具只枚举路由，不需要对象存储；关掉可避开 application-dev.yml 里
                // 那个 Windows 形态的 oss.s3.endpoint 在非 Windows 上解析成 /d: 的问题
                "oss.s3.enabled=false",
                "server.port=0",
                "spring.aot.enabled=false",
                "spring.autoconfigure.exclude=com.github.zeng.alt.security.rbac.client.config.RbacClientAutoConfiguration"
        }
)
public class HttpResourceSqlExporter {

    @TestConfiguration
    static class AuditConfig {
        @Bean
        public AuditorAware<String> auditorAware() {
            return () -> Optional.of("system");
        }
    }

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void exportSql() throws IOException {
        Map<String, Set<String>> routes = new LinkedHashMap<>();

        collectRequestMappingRoutes(routes);
        collectGeneratedRouterFileRoutes(routes);

        System.out.println();
        System.out.println("-- =============================================");
        System.out.println("-- Auto-generated HttpResource INSERT statements");
        System.out.println("-- Source: RequestMappingHandlerMapping + generated *Router.java");
        System.out.println("-- Generated: " + new java.util.Date());
        System.out.println("-- =============================================");
        System.out.println();

        long id = 100_000;
        for (Map.Entry<String, Set<String>> entry : routes.entrySet()) {
            String pattern = entry.getKey();
            for (String method : entry.getValue()) {
                String code = generateCode(pattern, method);
                String name = pattern + " " + method;

                System.out.printf(
                        "INSERT INTO main_permission (permission_id, resource_type, code, name, path, method, is_enabled, is_deleted, created_by, created_date, menu_id) " +
                                "VALUES (%d, 'HTTP', '%s', '%s', '%s', '%s', true, false, 'system', NOW(), NULL);%n",
                        id++, escape(code), escape(name), escape(pattern), method
                );
            }
        }

        System.out.println();
        System.out.println("-- Total: " + routes.values().stream().mapToInt(Set::size).sum() + " HttpResource entries");
        System.out.println();
    }

    private void collectRequestMappingRoutes(Map<String, Set<String>> routes) {
        Map<RequestMappingInfo, ?> methods = requestMappingHandlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, ?> entry : methods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            Set<String> patterns = info.getPatternsCondition() != null
                    ? info.getPatternsCondition().getPatterns()
                    : Set.of();
            Set<RequestMethod> httpMethods = info.getMethodsCondition() != null
                    ? info.getMethodsCondition().getMethods()
                    : Set.of(RequestMethod.GET, RequestMethod.POST,
                    RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH);

            for (String pattern : patterns) {
                routes.computeIfAbsent(pattern, k -> new TreeSet<>());
                for (RequestMethod httpMethod : httpMethods) {
                    routes.get(pattern).add(httpMethod.name());
                }
            }
        }
    }

    private void collectGeneratedRouterFileRoutes(Map<String, Set<String>> routes) throws IOException {
        Path generatedDir = Paths.get("build", "generated", "sources", "annotationProcessor", "java", "main");
        if (!Files.isDirectory(generatedDir)) {
            System.out.println("-- WARN: Generated sources directory not found: " + generatedDir.toAbsolutePath());
            return;
        }

        Pattern routePattern = Pattern.compile("\\.(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\(\"([^\"]+)\"");

        Files.walk(generatedDir)
                .filter(p -> p.toString().endsWith("Router.java"))
                .forEach(routerFile -> {
                    try {
                        String content = Files.readString(routerFile);
                        Matcher matcher = routePattern.matcher(content);
                        while (matcher.find()) {
                            String method = matcher.group(1);
                            String path = matcher.group(2);
                            routes.computeIfAbsent(path, k -> new TreeSet<>());
                            routes.get(path).add(method);
                        }
                    } catch (IOException e) {
                        System.err.println("-- ERROR reading " + routerFile + ": " + e.getMessage());
                    }
                });
    }

    private static String escape(String s) {
        return s.replace("'", "''");
    }

    private static String generateCode(String path, String method) {
        String s = path.replaceAll("/+", "/")
                .replaceAll("^/", "")
                .replaceAll("/", ".")
                .replaceAll("[^a-zA-Z0-9._{}\\-]", "_");
        if (s.isEmpty()) s = "root";
        String code = s + ":" + method;
        return code.length() > 64 ? code.substring(0, 64) : code;
    }
}
