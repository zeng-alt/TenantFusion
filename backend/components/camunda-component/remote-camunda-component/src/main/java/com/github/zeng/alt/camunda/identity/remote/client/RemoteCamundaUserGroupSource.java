package com.github.zeng.alt.camunda.identity.remote.client;

import com.github.zeng.alt.camunda.identity.api.CamundaIdentityGroup;
import com.github.zeng.alt.camunda.identity.api.CamundaIdentityUser;
import com.github.zeng.alt.camunda.identity.api.CamundaUserGroupSource;
import com.github.zeng.alt.camunda.identity.remote.dto.RemoteCamundaIdentityGroup;
import com.github.zeng.alt.camunda.identity.remote.dto.RemoteCamundaIdentityUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Remote implementation of CamundaUserGroupSource, calling admin service.
 */
@Component
public class RemoteCamundaUserGroupSource implements CamundaUserGroupSource {

    private final AdminIdentityClient client;

    public RemoteCamundaUserGroupSource(AdminIdentityClient client) {
        this.client = client;
    }

    @Override
    public Optional<CamundaIdentityUser> findByUsername(String username) {
        RemoteCamundaIdentityUser user = client.findUserByUsername(username);
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(toUser(user));
    }

    @Override
    public boolean matchesPassword(String username, String rawPassword) {
        return client.validatePassword(username, rawPassword);
    }

    @Override
    public List<CamundaIdentityGroup> findGroupsByUsername(String username) {
        RemoteCamundaIdentityUser user = client.findUserByUsername(username);
        if (user == null || user.groups() == null) {
            return List.of();
        }
        return user.groups().stream().map(this::toGroup).toList();
    }

    @Override
    public List<CamundaIdentityUser> findUsersByGroupCode(String code) {
        // Remote API does not support reverse lookup by group; fall back to all groups
        return List.of();
    }

    @Override
    public Optional<CamundaIdentityGroup> findByGroupCode(String code) {
        List<RemoteCamundaIdentityGroup> groups = client.findAllGroups();
        if (groups == null) {
            return Optional.empty();
        }
        return groups.stream()
                .filter(g -> g.id().equals(code))
                .findFirst()
                .map(this::toGroup);
    }

    @Override
    public List<CamundaIdentityGroup> findAllGroups() {
        List<RemoteCamundaIdentityGroup> groups = client.findAllGroups();
        if (groups == null) {
            return List.of();
        }
        return groups.stream().map(this::toGroup).toList();
    }

    private CamundaIdentityUser toUser(RemoteCamundaIdentityUser user) {
        return new CamundaIdentityUser(user.id(), user.firstName(), user.lastName(), user.email());
    }

    private CamundaIdentityGroup toGroup(RemoteCamundaIdentityGroup group) {
        return new CamundaIdentityGroup(group.id(), group.name(), group.type());
    }
}
