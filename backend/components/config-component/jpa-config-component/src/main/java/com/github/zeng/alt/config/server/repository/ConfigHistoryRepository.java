package com.github.zeng.alt.config.server.repository;

import com.github.zeng.alt.config.server.entity.ConfigHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigHistoryRepository extends JpaRepository<ConfigHistoryEntity, Long> {

    List<ConfigHistoryEntity> findByConfigIdOrderByVersionDesc(Long configId);
}
