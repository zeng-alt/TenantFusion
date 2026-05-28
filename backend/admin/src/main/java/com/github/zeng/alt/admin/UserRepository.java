package com.github.zeng.alt.admin;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.rest.annotation.CrudRest;
import org.springframework.stereotype.Repository;

/**
 * @author zengJiaJun
 * @since 2026年05月28日
 * @version 1.0
 */
@Repository
@CrudRest(path = "/user")
public interface UserRepository extends BaseRepository<UserEntity, Long> {
}
