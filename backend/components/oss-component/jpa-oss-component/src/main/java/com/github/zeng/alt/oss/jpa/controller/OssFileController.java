package com.github.zeng.alt.oss.jpa.controller;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.domain.base.BasePage;
import com.github.zeng.alt.oss.jpa.entity.OssFileEntity;
import com.github.zeng.alt.oss.jpa.repository.OssFileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Option;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.log.LogMessage;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OSS 文件记录 CRUD 控制器。
 * <p>
 * 当引入 {@code jpa-oss-component} 后自动注册，提供文件记录的基本增删改查接口。
 * 可通过 {@code oss.s3.crud.enabled=false} 禁用。
 * <p>
 * 如需扩展或自定义行为，继承此类并覆盖对应方法即可。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@CommonsLog
@RestController
@RequestMapping("/oss-files")
@Tag(name = "OSS 文件记录管理")
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
@ConditionalOnProperty(prefix = "oss.s3.crud", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OssFileController {

    protected final OssFileRepository repository;

    public OssFileController(OssFileRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "分页查询文件记录")
    public PageRestResponse<OssFileEntity> list(OssFileQuery query) {
        Page<OssFileEntity> pageResult = repository.findAll(query.toPage());
        return PageRestResponse.of(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                query.getPageSize(),
                query.getPage()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文件记录详情")
    public ResponseEntity<RestResponse<OssFileEntity>> detail(@PathVariable Long id) {
        Option<OssFileEntity> result = repository.findById(id);
        return result.map(entity -> ResponseEntity.ok(RestResponse.success(entity)))
                .getOrElse(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "创建文件记录")
    public RestResponse<OssFileEntity> create(@RequestBody OssFileEntity entity) {
        OssFileEntity saved = repository.save(entity);
        log.debug(LogMessage.format("OSS file record created: id={}, fileName={}", saved.getFileId(), saved.getFileName()));
        return RestResponse.success(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文件记录")
    public ResponseEntity<RestResponse<OssFileEntity>> update(@PathVariable Long id, @RequestBody OssFileEntity entity) {
        Option<OssFileEntity> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        entity.setFileId(id);
        OssFileEntity saved = repository.save(entity);
        return ResponseEntity.ok(RestResponse.success(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件记录")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable Long id) {
        Option<OssFileEntity> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.ok(RestResponse.success());
    }

    /**
     * OSS 文件分页查询参数。
     * <p>
     * 支持按文件名、原始文件名、存储类型过滤。
     * 可继承此类添加更多查询条件。
     */
    public static class OssFileQuery extends BasePage {

        private String fileName;
        private String originalFileName;
        private String storageType;
        private Integer status;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }

        public String getStorageType() {
            return storageType;
        }

        public void setStorageType(String storageType) {
            this.storageType = storageType;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}
