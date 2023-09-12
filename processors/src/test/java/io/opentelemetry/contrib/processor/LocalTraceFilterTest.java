/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LocalTraceFilterTest {

  @ParameterizedTest
  @MethodSource("rootSpanSlowerThanArguments")
  void rootSpanSlowerThan(long thresholdNanos, SpanData rootSpan, boolean expectedResult) {
    Predicate<LocalTraceData> predicate =
        LocalTraceFilter.rootSpanSlowerThan(Duration.ofNanos(thresholdNanos));
    assertThat(predicate.test(LocalTraceData.create(Collections.singletonList(rootSpan), rootSpan)))
        .isEqualTo(expectedResult);
  }

  private static Stream<Arguments> rootSpanSlowerThanArguments() {
    return Stream.of(
        arguments(
            500, testSpanBuilder().setStartEpochNanos(0).setEndEpochNanos(1000).build(), true),
        arguments(
            500, testSpanBuilder().setStartEpochNanos(0).setEndEpochNanos(100).build(), false),
        arguments(
            500, testSpanBuilder().setStartEpochNanos(0).setEndEpochNanos(500).build(), false));
  }

  @ParameterizedTest
  @MethodSource("rootSpanStatusArguments")
  void rootSpanStatus(StatusCode statusCode, SpanData rootSpan, boolean expectedResult) {
    Predicate<LocalTraceData> predicate = LocalTraceFilter.rootSpanStatus(statusCode);
    assertThat(predicate.test(LocalTraceData.create(Collections.singletonList(rootSpan), rootSpan)))
        .isEqualTo(expectedResult);
  }

  private static Stream<Arguments> rootSpanStatusArguments() {
    return Stream.of(
        arguments(StatusCode.ERROR, testSpanBuilder().setStatus(StatusData.unset()).build(), false),
        arguments(StatusCode.ERROR, testSpanBuilder().setStatus(StatusData.ok()).build(), false),
        arguments(StatusCode.ERROR, testSpanBuilder().setStatus(StatusData.error()).build(), true),
        arguments(StatusCode.UNSET, testSpanBuilder().setStatus(StatusData.unset()).build(), true),
        arguments(StatusCode.OK, testSpanBuilder().setStatus(StatusData.ok()).build(), true));
  }

  @ParameterizedTest
  @MethodSource("rootSpanNameMatches")
  void rootSpanNameMatches(
      Predicate<String> spanNameMatcher, SpanData rootSpan, boolean expectedResult) {
    Predicate<LocalTraceData> predicate = LocalTraceFilter.rootSpanNameMatches(spanNameMatcher);
    assertThat(predicate.test(LocalTraceData.create(Collections.singletonList(rootSpan), rootSpan)))
        .isEqualTo(expectedResult);
  }

  private static Stream<Arguments> rootSpanNameMatches() {
    return Stream.of(
        arguments(
            matcher(name -> name.equals("foo")), testSpanBuilder().setName("foo").build(), true),
        arguments(
            matcher(name -> name.equals("foo")), testSpanBuilder().setName("bar").build(), false),
        arguments(
            matcher(name -> name.matches(".*health.*")).negate(),
            testSpanBuilder().setName("GET /health").build(),
            false),
        arguments(
            matcher(name -> name.matches(".*health.*")).negate(),
            testSpanBuilder().setName("GET /users").build(),
            true));
  }

  @SuppressWarnings("CanIgnoreReturnValueSuggester")
  private static Predicate<String> matcher(Predicate<String> spanNameMatcher) {
    return spanNameMatcher;
  }

  @ParameterizedTest
  @MethodSource("anyExceptionEvent")
  void anyExceptionEvent(List<SpanData> spanData, boolean expectedResult) {
    Predicate<LocalTraceData> predicate = LocalTraceFilter.anyExceptionEvent();
    assertThat(predicate.test(LocalTraceData.create(spanData, spanData.get(0))))
        .isEqualTo(expectedResult);
  }

  private static Stream<Arguments> anyExceptionEvent() {
    return Stream.of(
        arguments(
            Arrays.asList(testSpanBuilder().build(), testSpanBuilder().build()),
            false,
            Arrays.asList(
                testSpanBuilder().setEvents(Collections.singletonList(exceptionEvent())).build(),
                testSpanBuilder().build()),
            true));
  }

  private static EventData exceptionEvent() {
    return EventData.create(0, "exception", Attributes.empty());
  }

  @ParameterizedTest
  @MethodSource("ratioBased")
  void ratioBased(double ratio) {
    Predicate<LocalTraceData> predicate = LocalTraceFilter.ratioBased(ratio);

    int successes = 0;
    SpanData localRoot = testSpanBuilder().build();
    LocalTraceData localTraceData =
        LocalTraceData.create(Collections.singletonList(localRoot), localRoot);
    for (int i = 0; i < 1000; i++) {
      if (predicate.test(localTraceData)) {
        successes++;
      }
    }

    double resultRatio = successes / 1000.0;
    assertThat(resultRatio).isCloseTo(ratio, Offset.offset(.1));
  }

  private static Stream<Arguments> ratioBased() {
    return Stream.of(arguments(0.0), arguments(0.5), arguments(1.0));
  }

  @Test
  void composite() {
    Predicate<LocalTraceData> errorStatus = LocalTraceFilter.rootSpanStatus(StatusCode.ERROR);
    Predicate<LocalTraceData> slow = LocalTraceFilter.rootSpanSlowerThan(Duration.ofNanos(500));
    Predicate<LocalTraceData> slowWithErrorStatus = errorStatus.and(slow);

    SpanData slowSpan = testSpanBuilder().setStartEpochNanos(0).setEndEpochNanos(1000).build();
    SpanData errorStatusSpan = testSpanBuilder().setStatus(StatusData.error()).build();
    SpanData slowAndErrorStatusSpan =
        testSpanBuilder()
            .setStartEpochNanos(0)
            .setEndEpochNanos(1000)
            .setStatus(StatusData.error())
            .build();

    assertThat(
            slowWithErrorStatus.test(
                LocalTraceData.create(Collections.singletonList(slowSpan), slowSpan)))
        .isFalse();
    assertThat(
            slowWithErrorStatus.test(
                LocalTraceData.create(Collections.singletonList(errorStatusSpan), errorStatusSpan)))
        .isFalse();
    assertThat(
            slowWithErrorStatus.test(
                LocalTraceData.create(
                    Collections.singletonList(slowAndErrorStatusSpan), slowAndErrorStatusSpan)))
        .isTrue();
  }

  private static TestSpanData.Builder testSpanBuilder() {
    return TestSpanData.builder()
        .setStartEpochNanos(0)
        .setEndEpochNanos(1)
        .setHasEnded(true)
        .setName("test")
        .setKind(SpanKind.INTERNAL)
        .setStatus(StatusData.unset());
  }
}
