/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.processor;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collection;

/**
 * A local trace represents the group of spans that are descendants of a particular {@link
 * #getLocalRootSpanData()}.
 */
@AutoValue
public abstract class LocalTraceData {

  public static LocalTraceData create(Collection<SpanData> spanData, SpanData localRootSpanData) {
    return new AutoValue_LocalTraceData(spanData, localRootSpanData);
  }

  LocalTraceData() {}

  /** Returns all spans in the local trace, including the {@link #getLocalRootSpanData()}. */
  public abstract Collection<SpanData> getSpanData();

  /** Return the local root span of the local trace. */
  public abstract SpanData getLocalRootSpanData();
}
