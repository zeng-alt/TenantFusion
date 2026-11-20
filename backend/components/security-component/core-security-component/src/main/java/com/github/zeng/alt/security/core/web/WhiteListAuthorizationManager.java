package com.github.zeng.alt.security.core.web;


import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.WhiteListTemplate;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月27日 21:33
 */
public class WhiteListAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final WhiteListTemplate whiteList;
    private final AuthorizationManager<RequestAuthorizationContext> fallback;

    public WhiteListAuthorizationManager(WhiteListService whiteListService) {
        this(new WhiteListTemplate(whiteListService), (authentication, context) -> new AuthorizationDecision(false));
        refreshWhiteList();
    }

    public WhiteListAuthorizationManager(
            WhiteListTemplate whiteList,
            AuthorizationManager<RequestAuthorizationContext> fallback
    ) {
        this.whiteList = whiteList;
        this.fallback = fallback;
    }

    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        return new AuthorizationDecision(switch (whiteList.resolve(context.getRequest())) {
            case PERMIT_ALL -> true;
            case AUTHENTICATED -> AuthenticatedAuthorizationManager.authenticated()
                    .authorize(authentication, context).isGranted();
            case PROTECTED -> fallback.authorize(authentication, context).isGranted();
        });
    }

    /** 白名单变更在下一次授权时生效，仅在快照变化时重新编译路径。 */
    public void refreshWhiteList() {
        whiteList.refresh();
    }

    public static AuthorizationManager<RequestAuthorizationContext> authenticated(WhiteListService whiteListService) {
        return new WhiteListAuthorizationManager(whiteListService);
    }
}
