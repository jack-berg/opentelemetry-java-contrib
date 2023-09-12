/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.processor;

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

class LocalTrace {
  private final AtomicInteger openSpans = new AtomicInteger(0);

  @GuardedBy("this")
  private final List<SpanData> spans = new ArrayList<>();

  @GuardedBy("this")
  @Nullable
  private SpanData localRootSpan;

  private final String localRootSpanId;

  LocalTrace(String localRootSpanId) {
    this.localRootSpanId = localRootSpanId;
  }

  void openSpan() {
    openSpans.incrementAndGet();
  }

  synchronized int closeSpan(SpanData spanData) {
    this.spans.add(spanData);
    if (spanData.getSpanContext().getSpanId().equals(localRootSpanId)) {
      this.localRootSpan = spanData;
    }
    return openSpans.decrementAndGet();
  }

  synchronized LocalTraceData getLocalTraceData() {
    if (localRootSpan == null) {
      throw new IllegalStateException(
          "Fetching local trace data before local root ended. This is likely a programming bug.");
    }
    return LocalTraceData.create(Collections.unmodifiableList(spans), localRootSpan);
  }

  String getLocalRootSpanId() {
    return localRootSpanId;
  }
}
