package com.github.zeng.alt.oss.jdbc.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.oss.jdbc.dao.OssFileDao;
import com.github.zeng.alt.oss.jdbc.dao.OssFileRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * OSS 文件记录 CRUD 控制器（JDBC 版本）。
 * <p>
 * 与 JPA 版本的 {@code OssFileController} 提供相同 API 契约，
 * 但底层基于 {@link OssFileDao}（NamedParameterJdbcTemplate）。
 * 可通过 {@code oss.s3.crud.enabled=false} 禁用。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@RestController
@RequestMapping("/oss-files")
@Tag(name = "OSS 文件记录管理（JDBC）")
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
@ConditionalOnProperty(prefix = "oss.s3.crud", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcOssFileController {

    private static final Logger log = LoggerFactory.getLogger(JdbcOssFileController.class);

    protected final OssFileDao ossFileDao;

    public JdbcOssFileController(OssFileDao ossFileDao) {
        this.ossFileDao = ossFileDao;
    }

    @GetMapping
    @Operation(summary = "分页查询文件记录")
    public PageRestResponse<OssFileRecord> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "file_id") String sort,
            @RequestParam(defaultValue = "desc") String order) {

        int offset = (page - 1) * pageSize;
        List<OssFileRecord> records = ossFileDao.findAll(offset, pageSize, sort, order);
        long total = ossFileDao.count();
        return PageRestResponse.of(records, total, pageSize, page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文件记录详情")
    public ResponseEntity<RestResponse<OssFileRecord>> detail(@PathVariable Long id) {
        Optional<OssFileRecord> result = ossFileDao.findById(id);
        return result.map(record -> ResponseEntity.ok(RestResponse.success(record)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "创建文件记录")
    public RestResponse<OssFileRecord> create(@RequestBody OssFileRecord record) {
        long id = ossFileDao.insert(record);
        OssFileRecord saved = ossFileDao.findById(id).orElse(record);
        log.debug("OSS file record created (JDBC): id={}, fileName={}", id, saved.getFileName());
        return RestResponse.success(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文件记录")
    public ResponseEntity<RestResponse<OssFileRecord>> update(@PathVariable Long id, @RequestBody OssFileRecord record) {
        Optional<OssFileRecord> existing = ossFileDao.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        record.setFileId(id);
        ossFileDao.update(record);
        OssFileRecord saved = ossFileDao.findById(id).orElse(record);
        return ResponseEntity.ok(RestResponse.success(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件记录")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable Long id) {
        Optional<OssFileRecord> existing = ossFileDao.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ossFileDao.deleteById(id);
        return ResponseEntity.ok(RestResponse.success());
    }
}
