@ApplicationModule(
        allowedDependencies = {"infrastructure::entity", "infrastructure::repository", "query::api", "query::dto"}
)
package com.github.zeng.alt.admin.query.service;

import org.springframework.modulith.ApplicationModule;