package com.github.zeng.alt.admin.infrastructure.repository;

import com.github.zeng.alt.admin.infrastructure.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {

    @Query("""
            select t from Tenant t
            where t.enabled = true
              and (t.deleted = false or t.deleted is null)
            """)
    List<Tenant> findAllActive();

    @Query("""
            select t from Tenant t
            where t.tenantId = :tenantId
              and t.enabled = true
              and (t.deleted = false or t.deleted is null)
            """)
    Optional<Tenant> findActiveById(String tenantId);
}
