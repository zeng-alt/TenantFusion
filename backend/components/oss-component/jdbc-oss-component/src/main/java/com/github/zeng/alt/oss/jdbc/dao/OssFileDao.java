package com.github.zeng.alt.oss.jdbc.dao;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * OSS 文件记录 JDBC DAO。
 * <p>
 * 提供基于 {@link NamedParameterJdbcTemplate} 的 {@code sys_oss_file} 表操作。
 * 作为 JPA 版本 {@code OssFileRepository} 的 JDBC 替代方案。
 *
 * @author zengJiaJun
 * @since 2026-07-02
 * @version 1.0
 */
@CommonsLog
public class OssFileDao {

    private static final String TABLE = "sys_oss_file";

    private static final String ALL_COLUMNS = """
            file_id, file_name, original_file_name, file_suffix, file_size,
            content_type, bucket_name, etag, md5, url, storage_type,
            status, tenant_id, created_by, created_date, last_modified_by, last_modified_date
            """;

    private static final RowMapper<OssFileRecord> ROW_MAPPER =
            new BeanPropertyRowMapper<>(OssFileRecord.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OssFileDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 分页查询文件记录。
     */
    public List<OssFileRecord> findAll(int offset, int limit, String sort, String order) {
        String safeSort = sanitizeSort(sort);
        String safeOrder = "desc".equalsIgnoreCase(order) ? "DESC" : "ASC";
        String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE
                + " ORDER BY " + safeSort + " " + safeOrder
                + " LIMIT :limit OFFSET :offset";
        return jdbcTemplate.query(sql,
                new MapSqlParameterSource()
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                ROW_MAPPER);
    }

    /**
     * 统计总数。
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + TABLE;
        Long result = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return result != null ? result : 0L;
    }

    /**
     * 根据 ID 查询。
     */
    public Optional<OssFileRecord> findById(Long id) {
        String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE + " WHERE file_id = :id";
        List<OssFileRecord> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("id", id), ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 插入文件记录。
     *
     * @return 自增主键 ID
     */
    public long insert(OssFileRecord record) {
        String sql = """
                INSERT INTO """ + TABLE + """
                (file_name, original_file_name, file_suffix, file_size, content_type,
                 bucket_name, etag, md5, url, storage_type, status, tenant_id,
                 created_by, created_date, last_modified_by, last_modified_date)
                VALUES (:fileName, :originalFileName, :fileSuffix, :fileSize, :contentType,
                        :bucketName, :etag, :md5, :url, :storageType, :status, :tenantId,
                        :createdBy, :createdDate, :lastModifiedBy, :lastModifiedDate)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new BeanPropertySqlParameterSource(record), keyHolder);
        Number key = keyHolder.getKey();
        long id = key != null ? key.longValue() : 0L;
        record.setFileId(id);
        return id;
    }

    /**
     * 更新文件记录。
     *
     * @return 更新行数
     */
    public int update(OssFileRecord record) {
        String sql = """
                UPDATE """ + TABLE + """
                SET file_name = :fileName, original_file_name = :originalFileName,
                    file_suffix = :fileSuffix, file_size = :fileSize,
                    content_type = :contentType, bucket_name = :bucketName,
                    etag = :etag, md5 = :md5, url = :url, storage_type = :storageType,
                    status = :status, tenant_id = :tenantId,
                    last_modified_by = :lastModifiedBy, last_modified_date = :lastModifiedDate
                WHERE file_id = :fileId
                """;
        return jdbcTemplate.update(sql, new BeanPropertySqlParameterSource(record));
    }

    /**
     * 根据 ID 删除。
     */
    public int deleteById(Long id) {
        String sql = "DELETE FROM " + TABLE + " WHERE file_id = :id";
        return jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    /**
     * 根据文件名查询。
     */
    public List<OssFileRecord> findByFileName(String fileName) {
        String sql = "SELECT " + ALL_COLUMNS + " FROM " + TABLE + " WHERE file_name = :fileName";
        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("fileName", fileName), ROW_MAPPER);
    }

    /**
     * 防止 SQL 注入：只允许已知的安全排序列名。
     */
    private String sanitizeSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "file_id";
        }
        // 只允许字母数字和下划线
        String sanitized = sort.replaceAll("[^a-zA-Z0-9_]", "");
        return sanitized.isEmpty() ? "file_id" : sanitized;
    }
}
