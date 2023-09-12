/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.processor;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.Random;
import java.util.function.Predicate;

public class LocalTraceFilter {

  private LocalTraceFilter() {}

  public static Predicate<LocalTraceData> rootSpanSlowerThan(Duration rootSpanDurationThreshold) {
    long thresholdNanos = rootSpanDurationThreshold.toNanos();
    return localTraceData -> {
      SpanData localRoot = localTraceData.getLocalRootSpanData();
      return localRoot.getEndEpochNanos() - localRoot.getStartEpochNanos() > thresholdNanos;
    };
  }

  public static Predicate<LocalTraceData> rootSpanStatus(StatusCode statusCode) {
    return localTraceData ->
        statusCode.equals(localTraceData.getLocalRootSpanData().getStatus().getStatusCode());
  }

  public static Predicate<LocalTraceData> rootSpanNameMatches(Predicate<String> spanNameMatcher) {
    return localTraceData -> spanNameMatcher.test(localTraceData.getLocalRootSpanData().getName());
  }

  public static Predicate<LocalTraceData> anyExceptionEvent() {
    return localTraceData ->
        localTraceData.getSpanData().stream()
            .flatMap(spanData -> spanData.getEvents().stream())
            .anyMatch(eventData -> eventData.getName().equals("exception"));
  }

  public static Predicate<LocalTraceData> ratioBased(double ratio) {
    if (ratio < 0.0 || ratio > 1.0) {
      throw new IllegalArgumentException("Ratio must be in range [0.0, 1.0]");
    }
    Random random = new Random();
    // Normalize to compare to random range [0, 1_000_000]
    double normalizedRatio = ratio * 1_000_000;
    return localTraceData -> normalizedRatio > random.nextInt(1_000_000);
  }
}
