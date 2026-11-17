package com.github.zeng.alt.json;

import com.github.zeng.alt.json.jackson3.BusinessJacksonModule;
import com.github.zeng.alt.json.jackson3.VavrJacksonModule;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vavr 泛型和 JSON 结构的往返契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class VavrJacksonModuleTest {
    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new BusinessJacksonModule()).addModule(new VavrJacksonModule()).build();

    @ParameterizedTest
    @MethodSource("values")
    void roundTripsImmutableValues(Object value, TypeReference<?> type) {
        final String json = mapper.writeValueAsString(value);
        assertThat((Object) mapper.readValue(json, type)).isEqualTo(value);
    }

    static Stream<Arguments> values() {
        return Stream.of(
                Arguments.of(Option.some("x"), new TypeReference<Option<String>>() {}),
                Arguments.of(Option.none(), new TypeReference<Option<String>>() {}),
                Arguments.of(Either.left("failure"), new TypeReference<Either<String, Integer>>() {}),
                Arguments.of(Either.right(3), new TypeReference<Either<String, Integer>>() {}),
                Arguments.of(io.vavr.collection.List.of(1, 2),
                        new TypeReference<io.vavr.collection.List<Integer>>() {}),
                Arguments.of(io.vavr.collection.Array.of(1, 2),
                        new TypeReference<io.vavr.collection.Array<Integer>>() {}),
                Arguments.of(io.vavr.collection.Vector.of(1, 2),
                        new TypeReference<io.vavr.collection.Vector<Integer>>() {}),
                Arguments.of(io.vavr.collection.Stream.of(1, 2),
                        new TypeReference<io.vavr.collection.Stream<Integer>>() {}),
                Arguments.of(io.vavr.collection.Queue.of(1, 2),
                        new TypeReference<io.vavr.collection.Queue<Integer>>() {}),
                Arguments.of(io.vavr.collection.PriorityQueue.of(2, 1),
                        new TypeReference<io.vavr.collection.PriorityQueue<Integer>>() {}),
                Arguments.of(io.vavr.collection.HashSet.of(1, 2),
                        new TypeReference<io.vavr.collection.Set<Integer>>() {}),
                Arguments.of(io.vavr.collection.LinkedHashSet.of(2, 1),
                        new TypeReference<io.vavr.collection.LinkedHashSet<Integer>>() {}),
                Arguments.of(io.vavr.collection.TreeSet.of(2, 1),
                        new TypeReference<io.vavr.collection.SortedSet<Integer>>() {}),
                Arguments.of(io.vavr.collection.CharSeq.of("abc"),
                        new TypeReference<io.vavr.collection.CharSeq>() {}),
                Arguments.of(io.vavr.collection.HashMap.of(1, "one"),
                        new TypeReference<io.vavr.collection.Map<Integer, String>>() {}),
                Arguments.of(io.vavr.collection.LinkedHashMap.of(1, "one"),
                        new TypeReference<io.vavr.collection.LinkedHashMap<Integer, String>>() {}),
                Arguments.of(io.vavr.collection.TreeMap.of(1, "one"),
                        new TypeReference<io.vavr.collection.SortedMap<Integer, String>>() {}),
                Arguments.of(Tuple.empty(), new TypeReference<io.vavr.Tuple0>() {}),
                Arguments.of(Tuple.of("a"), new TypeReference<io.vavr.Tuple1<String>>() {}),
                Arguments.of(Tuple.of("a", 1), new TypeReference<io.vavr.Tuple2<String, Integer>>() {}),
                Arguments.of(Tuple.of(1, 2, 3), new TypeReference<io.vavr.Tuple3<Integer, Integer, Integer>>() {})
        );
    }

    @Test
    void preservesNestedGenericsAndMissingOptions() {
        final var type = new TypeReference<io.vavr.collection.Map<String, Option<Amount>>>() {};
        final var values = mapper.readValue("{\"a\":{\"value\":5},\"b\":null}", type);
        assertThat(values.get("a").get().get()).isEqualTo(new Amount(5));
        assertThat(values.get("b").get()).isEqualTo(Option.none());
        assertThat(mapper.readValue("{}", OptionalAmount.class).amount()).isEqualTo(Option.none());
        final Lazy<Amount> lazy = mapper.readValue("{\"value\":5}", new TypeReference<Lazy<Amount>>() {});
        assertThat(lazy.get()).isEqualTo(new Amount(5));
        assertThat(mapper.writeValueAsString(lazy)).isEqualTo("{\"value\":5}");
    }

    @Test
    void acceptsTheLegacyObjectFormOfEither() {
        final var type = new TypeReference<Either<String, Integer>>() {};
        assertThat(mapper.readValue("{\"left\":\"bad\"}", type)).isEqualTo(Either.left("bad"));
        assertThat(mapper.readValue("{\"r\":2}", type)).isEqualTo(Either.right(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[1]", "[\"unknown\",1]", "[1,2]"})
    void rejectsMalformedEitherValues(String json) {
        assertThatThrownBy(() -> mapper.readValue(json, new TypeReference<Either<String, Integer>>() {}))
                .isInstanceOf(tools.jackson.databind.exc.MismatchedInputException.class);
    }

    @Test
    void rejectsWrongCollectionAndTupleShapes() {
        assertThatThrownBy(() -> mapper.readValue("{}", new TypeReference<io.vavr.collection.List<Integer>>() {}))
                .isInstanceOf(tools.jackson.databind.exc.MismatchedInputException.class);
        assertThatThrownBy(() -> mapper.readValue("[1]", new TypeReference<io.vavr.Tuple2<Integer, String>>() {}))
                .isInstanceOf(tools.jackson.databind.exc.MismatchedInputException.class);
    }

    private record Amount(
        int value
    ) {
    }

    private record OptionalAmount(
        Option<Amount> amount
    ) {
    }
}
