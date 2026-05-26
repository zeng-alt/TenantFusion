package com.github.zeng.alt.api.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年11月11日 21:43
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TurnPageRestResponse<T, C extends Serializable> extends RestResponse<List<T>> {

    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean hasNext;
    private Boolean hasPre;
    private C currentCursor;
    private C nextCursor;
}
