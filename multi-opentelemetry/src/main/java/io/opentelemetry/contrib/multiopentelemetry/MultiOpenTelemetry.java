/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.multiopentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import java.util.List;
import java.util.stream.Collectors;

public class MultiOpenTelemetry implements OpenTelemetry {

  private final TracerProvider tracerProvider;

  private MultiOpenTelemetry(List<OpenTelemetry> delegates) {
    this.tracerProvider =
        new MultiTracerProvider(
            delegates.stream().map(OpenTelemetry::getTracerProvider).collect(Collectors.toList()));
  }

  public static OpenTelemetry create(List<OpenTelemetry> delegates) {
    return new MultiOpenTelemetry(delegates);
  }

  @Override
  public TracerProvider getTracerProvider() {
    return tracerProvider;
  }

  @Override
  public MeterProvider getMeterProvider() {
    // TODO: implement
    return MeterProvider.noop();
  }

  @Override
  public LoggerProvider getLogsBridge() {
    // TODO: implement
    return LoggerProvider.noop();
  }

  @Override
  public ContextPropagators getPropagators() {
    // TODO: implement
    return ContextPropagators.noop();
  }
}
