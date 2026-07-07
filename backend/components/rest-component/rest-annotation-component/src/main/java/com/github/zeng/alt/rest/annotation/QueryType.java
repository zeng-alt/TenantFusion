package com.github.zeng.alt.rest.annotation;

public enum QueryType {
    EQ,         // =
    LIKE,       // like %xx%
    LEFT_LIKE,  // xx%
    RIGHT_LIKE, // %xx
    GT,         // >
    GTE,        // >=
    LT,         // <
    LTE,        // <=
    IN,         // in
    BETWEEN     // between
}