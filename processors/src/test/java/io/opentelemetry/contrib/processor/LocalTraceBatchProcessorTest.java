/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.processor;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.testing.time.TestClock;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalTraceBatchProcessorTest {

  private static final TestClock clock = TestClock.create();

  private TestSpanExporter exporter;
  private Tracer tracer;

  @BeforeEach
  void setup() {
    exporter = new TestSpanExporter();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .setClock(clock)
            .setSampler(Sampler.alwaysOn())
            .addSpanProcessor(LocalTraceBatchProcessor.create(exporter))
            .build();
    tracer = tracerProvider.get("test-tracer");
  }

  @Test
  void syncCase() {
    Span a = tracer.spanBuilder("a").startSpan();
    try (Scope scopeA = a.makeCurrent()) {
      doWork(10);

      Span b = tracer.spanBuilder("b").startSpan();
      try (Scope scopeB = b.makeCurrent()) {
        doWork(10);

        Span d = tracer.spanBuilder("d").startSpan();
        try (Scope scopeD = d.makeCurrent()) {
          doWork(10);
          d.end();
        }

        b.end();
      }

      Span c = tracer.spanBuilder("c").startSpan();
      try (Scope scopeC = c.makeCurrent()) {
        doWork(10);
        c.end();
      }

      a.end();
    }

    Span e = tracer.spanBuilder("e").startSpan();
    try (Scope scopeE = e.makeCurrent()) {
      doWork(10);

      e.end();
    }

    assertThat(exporter.getSpanBatchesAndReset())
        .satisfiesExactlyInAnyOrder(
            spanBatch ->
                assertThat(spanBatch)
                    .satisfiesExactlyInAnyOrder(
                        span -> assertThat(span.getName()).isEqualTo("a"),
                        span -> assertThat(span.getName()).isEqualTo("b"),
                        span -> assertThat(span.getName()).isEqualTo("c"),
                        span -> assertThat(span.getName()).isEqualTo("d")),
            spanData ->
                assertThat(spanData)
                    .satisfiesExactly(span -> assertThat(span.getName()).isEqualTo("e")));
  }

  @Test
  void asyncCase() {
    ExecutorService executorService = Context.taskWrapping(Executors.newSingleThreadExecutor());
    List<Future<?>> futures = new ArrayList<>();

    Span a = tracer.spanBuilder("a").startSpan();
    try (Scope scopeA = a.makeCurrent()) {
      doWork(10);

      futures.add(
          executorService.submit(
              () -> {
                Span b = tracer.spanBuilder("b").startSpan();
                try (Scope scopeB = b.makeCurrent()) {
                  doWork(10);

                  Span c = tracer.spanBuilder("c").startSpan();
                  try (Scope scopeC = c.makeCurrent()) {
                    doWork(10);
                    c.end();
                  }

                  b.end();
                }
              }));

      sleep(10);

      a.end();
    }

    Span d = tracer.spanBuilder("d").startSpan();
    try (Scope scopeD = d.makeCurrent()) {
      doWork(10);

      d.end();
    }

    // Wait for async tasks to complete
    awaitAllFutures(futures);

    assertThat(exporter.getSpanBatchesAndReset())
        .satisfiesExactlyInAnyOrder(
            spanBatch ->
                assertThat(spanBatch)
                    .satisfiesExactlyInAnyOrder(
                        span -> assertThat(span.getName()).isEqualTo("a"),
                        span -> assertThat(span.getName()).isEqualTo("b"),
                        span -> assertThat(span.getName()).isEqualTo("c")),
            spanData ->
                assertThat(spanData)
                    .satisfiesExactly(span -> assertThat(span.getName()).isEqualTo("d")));
  }

  @Test
  void localParent() {
    // Create parent SpanContext representing with remote=false
    SpanContext spanContext =
        SpanContext.create(
            IdGenerator.random().generateTraceId(),
            IdGenerator.random().generateSpanId(),
            TraceFlags.getDefault(),
            TraceState.getDefault());
    Context parentContext = Context.current().with(Span.wrap(spanContext));
    try (Scope parentScope = parentContext.makeCurrent()) {

      Span a = tracer.spanBuilder("a").setParent(parentContext).startSpan();
      try (Scope scopeA = a.makeCurrent()) {
        doWork(10);

        a.end();
      }
    }

    // Span should be dropped by processor since we never
    assertThat(exporter.getSpanBatchesAndReset()).isEmpty();
  }

  private static void awaitAllFutures(List<Future<?>> futures) {
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private static void doWork(int millis) {
    clock.advance(Duration.ofMillis(millis));
  }

  private static final class TestSpanExporter implements SpanExporter {

    private final List<Collection<SpanData>> spanBatches = new ArrayList<>();

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
      this.spanBatches.add(spans);
      return CompletableResultCode.ofSuccess();
    }

    private List<Collection<SpanData>> getSpanBatchesAndReset() {
      List<Collection<SpanData>> result = new ArrayList<>(spanBatches);
      this.spanBatches.clear();
      return result;
    }

    @Override
    public CompletableResultCode flush() {
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
      return CompletableResultCode.ofSuccess();
    }
  }
}
