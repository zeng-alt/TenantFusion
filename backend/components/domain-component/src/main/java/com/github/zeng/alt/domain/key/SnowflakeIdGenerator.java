package com.github.zeng.alt.domain.key;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class SnowflakeIdGenerator implements IdentifierGenerator {

    private static final IdGenerator snowflake = new IdGenerator();

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return snowflake.nextId();
    }
}
