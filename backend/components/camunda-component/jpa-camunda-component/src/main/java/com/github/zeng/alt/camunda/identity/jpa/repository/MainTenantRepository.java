package com.github.zeng.alt.camunda.identity.jpa.repository;

import com.github.zeng.alt.camunda.identity.jpa.entity.MainTenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MainTenantRepository extends JpaRepository<MainTenantEntity, String> {

    @Query("""
            select t from MainTenantEntity t
            where t.tenantId = :tenantId
              and t.enabled = true
              and (t.deleted = false or t.deleted is null)
            """)
    Optional<MainTenantEntity> findActiveById(String tenantId);

    @Query("""
            select t from MainTenantEntity t
            where t.enabled = true
              and (t.deleted = false or t.deleted is null)
            order by t.tenantId
            """)
    List<MainTenantEntity> findAllActive();
}
