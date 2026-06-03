/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.multiopentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Context;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

class MultiTracerProvider implements TracerProvider {

  private final List<TracerProvider> delegates;

  MultiTracerProvider(List<TracerProvider> delegates) {
    this.delegates = delegates;
  }

  @Override
  public Tracer get(String instrumentationScopeName) {
    return new MultiTracer(
        delegates.stream()
            .map(provider -> provider.get(instrumentationScopeName))
            .collect(Collectors.toList()));
  }

  @Override
  public Tracer get(String instrumentationScopeName, String instrumentationScopeVersion) {
    return new MultiTracer(
        delegates.stream()
            .map(provider -> provider.get(instrumentationScopeName, instrumentationScopeVersion))
            .collect(Collectors.toList()));
  }

  private static class MultiTracer implements Tracer {

    private final List<Tracer> delegates;

    private MultiTracer(List<Tracer> delegates) {
      this.delegates = delegates;
    }

    @Override
    public boolean isEnabled() {
      return delegates.stream().anyMatch(Tracer::isEnabled);
    }

    @Override
    public SpanBuilder spanBuilder(String spanName) {
      return new MultiSpanBuilder(
          delegates.stream()
              .map(tracer -> tracer.spanBuilder(spanName))
              .collect(Collectors.toList()));
    }
  }

  private static class MultiSpanBuilder implements SpanBuilder {

    private final List<SpanBuilder> delegates;

    private MultiSpanBuilder(List<SpanBuilder> delegates) {
      this.delegates = delegates;
    }

    @Override
    public SpanBuilder setParent(Context context) {
      delegates.forEach(delegate -> delegate.setParent(context));
      return this;
    }

    @Override
    public SpanBuilder setNoParent() {
      delegates.forEach(SpanBuilder::setNoParent);
      return this;
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext) {
      delegates.forEach(delegate -> delegate.addLink(spanContext));
      return this;
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext, Attributes attributes) {
      delegates.forEach(delegate -> delegate.addLink(spanContext, attributes));
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, @Nullable String value) {
      delegates.forEach(delegate -> delegate.setAttribute(key, value));
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, long value) {
      delegates.forEach(delegate -> delegate.setAttribute(key, value));
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, double value) {
      delegates.forEach(delegate -> delegate.setAttribute(key, value));
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, boolean value) {
      delegates.forEach(delegate -> delegate.setAttribute(key, value));
      return this;
    }

    @Override
    public <T> SpanBuilder setAttribute(AttributeKey<T> key, @Nullable T value) {
      delegates.forEach(delegate -> delegate.setAttribute(key, value));
      return this;
    }

    @Override
    public SpanBuilder setSpanKind(SpanKind spanKind) {
      delegates.forEach(delegate -> delegate.setSpanKind(spanKind));
      return this;
    }

    @Override
    public SpanBuilder setStartTimestamp(long startTimestamp, TimeUnit unit) {
      delegates.forEach(delegate -> delegate.setStartTimestamp(startTimestamp, unit));
      return this;
    }

    @Override
    public Span startSpan() {
      return new MultiSpan(
          delegates.stream().map(SpanBuilder::startSpan).collect(Collectors.toList()));
    }
  }

  private static class MultiSpan implements Span {

    private final List<Span> delegates;

    private MultiSpan(List<Span> delegates) {
      this.delegates = delegates;
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, @Nullable T value) {
      delegates.forEach(delegate -> delegate.setAttribute(key, value));
      return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
      delegates.forEach(delegate -> delegate.addEvent(name, attributes));
      return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
      delegates.forEach(delegate -> delegate.addEvent(name, attributes, timestamp, unit));
      return this;
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
      delegates.forEach(delegate -> delegate.setStatus(statusCode, description));
      return this;
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
      delegates.forEach(delegate -> delegate.recordException(exception, additionalAttributes));
      return this;
    }

    @Override
    public Span updateName(String name) {
      delegates.forEach(delegate -> delegate.updateName(name));
      return this;
    }

    @Override
    public void end() {
      delegates.forEach(Span::end);
    }

    @Override
    public void end(long timestamp, TimeUnit unit) {
      delegates.forEach(delegate -> delegate.end(timestamp, unit));
    }

    @Override
    public SpanContext getSpanContext() {
      // TODO: review
      return delegates.getFirst().getSpanContext();
    }

    @Override
    public boolean isRecording() {
      return delegates.stream().anyMatch(Span::isRecording);
    }
  }
}
