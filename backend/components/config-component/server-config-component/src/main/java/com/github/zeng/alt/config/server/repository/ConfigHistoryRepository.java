package com.github.zeng.alt.config.server.repository;

import com.github.zeng.alt.config.server.entity.ConfigHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigHistoryRepository extends JpaRepository<ConfigHistoryEntity, Long> {

    List<ConfigHistoryEntity> findByConfigIdOrderByVersionDesc(Long configId);
}
