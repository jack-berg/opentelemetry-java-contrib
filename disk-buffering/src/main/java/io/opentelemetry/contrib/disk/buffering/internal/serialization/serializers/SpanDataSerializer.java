/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.ResourceSpansDataMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.spans.ResourceSpansData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

public final class SpanDataSerializer implements SignalSerializer<SpanData> {
  @Nullable private static SpanDataSerializer instance;

  private SpanDataSerializer() {}

  static SpanDataSerializer get() {
    if (instance == null) {
      instance = new SpanDataSerializer();
    }
    return instance;
  }

  @Override
  public byte[] serialize(Collection<SpanData> spanData) {
    try {
      return JsonSerializer.serialize(ResourceSpansDataMapper.INSTANCE.toJsonDto(spanData));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public List<SpanData> deserialize(byte[] source) {
    try {
      return ResourceSpansDataMapper.INSTANCE.fromJsonDto(
          JsonSerializer.deserialize(ResourceSpansData.class, source));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}