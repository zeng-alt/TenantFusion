package com.github.zeng.alt.camunda.identity.jpa.repository;

import com.github.zeng.alt.camunda.identity.jpa.entity.MainUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MainUserRepository extends JpaRepository<MainUserEntity, Long> {

    @Query("""
            select u from MainUserEntity u
            where u.username = :username
              and u.enabled = true
              and (u.deleted = false or u.deleted is null)
            """)
    Optional<MainUserEntity> findActiveByUsername(String username);

    @Query("""
            select count(u) from MainUserEntity u
            where u.enabled = true
              and (u.deleted = false or u.deleted is null)
            """)
    long countActive();
}
