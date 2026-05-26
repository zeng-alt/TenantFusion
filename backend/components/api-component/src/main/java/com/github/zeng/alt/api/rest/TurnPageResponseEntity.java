package com.github.zeng.alt.api.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;

import java.util.Collection;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年02月25日 21:41
 */
public class TurnPageResponseEntity<T, C> extends HttpEntityStatus<TurnPageEntity<Collection<T>, C>> {

    public TurnPageResponseEntity(HttpStatusCode status) {
        this(null, status);
    }

    public TurnPageResponseEntity(@Nullable TurnPageEntity<Collection<T>, C> body, HttpStatusCode status) {
        super(body, status.value());
    }

    public static <T, C> TurnPageResponseEntity<T, C> of(boolean hasNext, boolean hasPre, C currentCursor, C nextCursor, Collection<T> data) {
        TurnPageEntity<Collection<T>, C> turnPageEntity = new TurnPageEntity<>();
        turnPageEntity.setHasNext(hasNext);
        turnPageEntity.setHasPre(hasPre);
        turnPageEntity.setCurrentCursor(currentCursor);
        turnPageEntity.setNextCursor(nextCursor);
        turnPageEntity.setData(data);
        return new TurnPageResponseEntity<>(turnPageEntity, HttpStatus.OK);

    }
}
