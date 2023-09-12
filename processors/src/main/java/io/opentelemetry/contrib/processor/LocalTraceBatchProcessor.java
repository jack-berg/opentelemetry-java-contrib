/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.processor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A processor which groups spans into "local traces", where a local trace is comprised of all spans
 * descendant from the same local root span. A local root span is a span which either has no parent,
 * or has a remote parent.
 *
 * <p>The processor ensures spans part of the same local trace are passed to the {@link
 * SpanExporter} is the same batch.
 *
 * <p>The processor allows local traces to be filtered before passing to the {@link SpanExporter}.
 * This enables a form of tail sampling where the sampling decision can be made using all completed
 * spans comprising a local trace.
 *
 * <p>The processor will drop any spans it receives which are not local root spans, and which are
 * not a part of any open local root. This can occur as the result of instrumentation bugs, or if
 * asynchronous code causes child spans to be started after their parent has ended, or if {@link
 * SdkTracerProvider} is configured with a {@link Sampler} which does not record every span.
 */
public class LocalTraceBatchProcessor implements SpanProcessor {

  private final Logger logger = Logger.getLogger(LocalTraceBatchProcessor.class.getName());

  private final Map<String, LocalTrace> openSpansToLocalTrace = new ConcurrentHashMap<>();

  private final Predicate<LocalTraceData> localTraceFilter;
  private final SpanExporter spanExporter;

  private LocalTraceBatchProcessor(
      Predicate<LocalTraceData> localTraceFilter, SpanExporter spanExporter) {
    this.localTraceFilter = localTraceFilter;
    this.spanExporter = spanExporter;
  }

  /**
   * Create a {@link LocalTraceBatchProcessor}.
   *
   * @param localTraceFilter the filter to apply to local traces before pushed to {@code
   *     spanExporter}.
   * @param spanExporter the span exporter to which spans are pushed.
   */
  public static LocalTraceBatchProcessor create(
      Predicate<LocalTraceData> localTraceFilter, SpanExporter spanExporter) {
    return new LocalTraceBatchProcessor(localTraceFilter, spanExporter);
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
    if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
      LocalTrace localTrace = new LocalTrace(span.getSpanContext().getSpanId());
      localTrace.openSpan();
      openSpansToLocalTrace.put(span.getSpanContext().getSpanId(), localTrace);
      span.setAttribute("local_root_span_id", localTrace.getLocalRootSpanId());
      return;
    }
    LocalTrace localTrace = openSpansToLocalTrace.get(parentSpanContext.getSpanId());
    if (localTrace != null) {
      openSpansToLocalTrace.put(span.getSpanContext().getSpanId(), localTrace);
      localTrace.openSpan();
      span.setAttribute("local_root_span_id", localTrace.getLocalRootSpanId());
      return;
    }
    logger.log(
        Level.WARNING,
        "Span started which is not local root but has no open parent. Span context: {0}, parent context: {1}",
        new Object[] {span.getSpanContext(), parentSpanContext});
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    LocalTrace localTrace = openSpansToLocalTrace.remove(span.getSpanContext().getSpanId());
    if (localTrace == null) {
      logger.log(
          Level.WARNING,
          "Span ended with no open local trace. Span context: {0}",
          span.getSpanContext());
      return;
    }
    int openSpans = localTrace.closeSpan(span.toSpanData());
    if (openSpans == 0) {
      LocalTraceData localTraceData = localTrace.getLocalTraceData();
      logger.info(
          "Local trace with root span id "
              + localTraceData.getLocalRootSpanData().getSpanId()
              + " finished with "
              + localTraceData.getSpanData().size()
              + " spans. ");

      if (localTraceFilter.test(localTraceData)) {
        spanExporter.export(localTraceData.getSpanData());
      }
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  @Override
  public CompletableResultCode shutdown() {
    // TODO
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode forceFlush() {
    // TODO
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public void close() {
    shutdown().join(10, TimeUnit.SECONDS);
  }
}
