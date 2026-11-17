package com.github.zeng.alt.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@JacksonStdImpl
public class BigNumberConverter extends NumberSerializer {

    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    private static final long MIN_SAFE_INTEGER = -9007199254740991L;

    public static final BigNumberConverter INSTANCE = new BigNumberConverter(Number.class);

    public BigNumberConverter(Class<? extends Number> rawType) {
        super(rawType);
    }

    /** 判断整数能否按组件原有的 JavaScript 精度契约输出为数值。 */
    public static boolean isSafeInteger(Number value) {
        final BigInteger integer = value instanceof BigInteger bigInteger
                ? bigInteger : BigInteger.valueOf(value.longValue());
        return integer.compareTo(BigInteger.valueOf(MIN_SAFE_INTEGER)) > 0
                && integer.compareTo(BigInteger.valueOf(MAX_SAFE_INTEGER)) < 0;
    }

    @Override
    public void serialize(Number value, JsonGenerator g, SerializerProvider provider) throws IOException {
        if (isSafeInteger(value)) {
            super.serialize(value, g, provider);
        } else {
            g.writeString(value.toString());
        }
    }

}
