package com.github.zeng.alt.i18n;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月06日 14:51
 */
@FunctionalInterface
public interface ResponseAdviceProvider {

    Object handle(Object response);
}
