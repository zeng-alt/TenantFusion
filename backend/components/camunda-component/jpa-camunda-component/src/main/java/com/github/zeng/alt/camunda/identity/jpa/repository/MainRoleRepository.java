package com.github.zeng.alt.camunda.identity.jpa.repository;

import com.github.zeng.alt.camunda.identity.jpa.entity.MainRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MainRoleRepository extends JpaRepository<MainRoleEntity, Long> {

    @Query("""
            select r from MainRoleEntity r
            where r.code = :code
              and r.enabled = true
            """)
    Optional<MainRoleEntity> findActiveByCode(String code);

    @Query("""
            select r from MainRoleEntity r
            where r.enabled = true
            order by r.roleId
            """)
    List<MainRoleEntity> findAllActiveByOrderByRoleId();
}
