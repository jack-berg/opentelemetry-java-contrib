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
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.GuardedBy;

public class LocalTraceBatchProcessor implements SpanProcessor {

  private final Logger logger = Logger.getLogger(LocalTraceBatchProcessor.class.getName());

  private final Map<String, LocalTrace> openSpansToLocalTrace = new ConcurrentHashMap<>();

  private final SpanExporter spanExporter;

  private LocalTraceBatchProcessor(SpanExporter spanExporter) {
    this.spanExporter = spanExporter;
  }

  public static LocalTraceBatchProcessor create(SpanExporter spanExporter) {
    return new LocalTraceBatchProcessor(spanExporter);
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
    if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
      LocalTrace localTrace = new LocalTrace(span.getSpanContext().getSpanId());
      localTrace.openSpan();
      openSpansToLocalTrace.put(span.getSpanContext().getSpanId(), localTrace);
      span.setAttribute("local_root_span_id", localTrace.localRootSpanId);
      return;
    }
    LocalTrace localTrace = openSpansToLocalTrace.get(parentSpanContext.getSpanId());
    if (localTrace != null) {
      openSpansToLocalTrace.put(span.getSpanContext().getSpanId(), localTrace);
      localTrace.openSpan();
      span.setAttribute("local_root_span_id", localTrace.localRootSpanId);
      return;
    }
    logger.log(
        Level.WARNING,
        "Span started which is not local root and but has no open parent. Span context: {0}, parent context: {1}",
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
      List<SpanData> batch = localTrace.getSpans();
      logger.info(
          "Local trace with root span id "
              + localTrace.localRootSpanId
              + " finished with "
              + batch.size()
              + " spans. ");
      spanExporter.export(batch);
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

  private static class LocalTrace {
    private final AtomicInteger openSpans = new AtomicInteger(0);

    @GuardedBy("this")
    private final List<SpanData> spans = new ArrayList<>();

    private final String localRootSpanId;

    private LocalTrace(String localRootSpanId) {
      this.localRootSpanId = localRootSpanId;
    }

    private void openSpan() {
      openSpans.incrementAndGet();
    }

    private synchronized int closeSpan(SpanData spanData) {
      this.spans.add(spanData);
      return openSpans.decrementAndGet();
    }

    private synchronized List<SpanData> getSpans() {
      return spans;
    }
  }
}
