package com.github.zeng.alt.security.core.reactive;


import com.github.zeng.alt.security.api.WhiteListService;
import com.github.zeng.alt.security.core.WhiteListTemplate;
import org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年11月27日 21:33
 */
public class ReactiveWhiteListAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final WhiteListTemplate whiteList;
    private final ReactiveAuthorizationManager<AuthorizationContext> fallback;

    public ReactiveWhiteListAuthorizationManager(WhiteListService service) {
        this(new WhiteListTemplate(service), (authentication, context) -> Mono.just(new AuthorizationDecision(false)));
    }

    public ReactiveWhiteListAuthorizationManager(
            WhiteListTemplate whiteList,
            ReactiveAuthorizationManager<AuthorizationContext> fallback
    ) {
        this.whiteList = whiteList;
        this.fallback = fallback;
    }

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication, AuthorizationContext object) {
        return Mono.defer(() -> switch (whiteList.resolve(object.getExchange())) {
            case PERMIT_ALL -> Mono.just(new AuthorizationDecision(true));
            case AUTHENTICATED -> AuthenticatedReactiveAuthorizationManager.authenticated()
                    .authorize(authentication, object);
            case PROTECTED -> fallback.authorize(authentication, object);
        });
    }

    public static ReactiveAuthorizationManager<AuthorizationContext> authenticated(WhiteListService whiteListService) {
        return new ReactiveWhiteListAuthorizationManager(whiteListService);
    }
}
