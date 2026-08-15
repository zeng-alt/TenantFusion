package com.github.zeng.alt.camunda.identity.jpa.repository;

import com.github.zeng.alt.camunda.identity.jpa.entity.MainUserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MainUserRoleRepository extends JpaRepository<MainUserRoleEntity, Long> {

    @Query("""
            select ur from MainUserRoleEntity ur
            join fetch ur.role r
            where ur.user.username = :username
              and r.enabled = true
            """)
    List<MainUserRoleEntity> findActiveByUserUsername(String username);

    @Query("""
            select ur from MainUserRoleEntity ur
            join fetch ur.user u
            where ur.role.code = :code
              and u.enabled = true
              and (u.deleted = false or u.deleted is null)
            """)
    List<MainUserRoleEntity> findActiveByRoleCode(String code);
}
