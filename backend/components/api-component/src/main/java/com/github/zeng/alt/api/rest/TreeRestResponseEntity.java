package com.github.zeng.alt.api.rest;

import com.zjj.autoconfigure.component.core.Response;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年02月25日 16:49
 */
public class TreeRestResponseEntity<T extends Parent<P>, P extends Comparable<P>> extends RestResponse<TreeEntity<T, P>> {
}
