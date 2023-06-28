/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.internal.exporters.AbstractDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.exporter.internal.otlp.traces.ResourceSpansMarshaler;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfoBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * This is a {@link SpanExporter} wrapper that takes care of intercepting all the signals sent out
 * to be exported, tries to store them in the disk in order to export them later.
 *
 * <p>In order to use it, you need to wrap your own {@link SpanExporter} with a new instance of this
 * one, which will be the one you need to register in your {@link SpanProcessor}.
 */
public final class SpanDiskExporter extends AbstractDiskExporter<SpanData> implements SpanExporter {
  private final SpanDeserializer spanDeserializer = new SpanDeserializer();
  private final SpanExporter wrapped;

  /**
   * @param wrapped - Your own exporter.
   * @param rootDir - The directory to create this signal's cache dir where all the data will be
   *     written into.
   * @param configuration - How you want to manage the storage process.
   */
  public SpanDiskExporter(SpanExporter wrapped, File rootDir, StorageConfiguration configuration) {
    super(rootDir, configuration);
    this.wrapped = wrapped;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    return onExport(spans);
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      onShutDown();
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    } finally {
      wrapped.shutdown();
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  protected String getStorageFolderName() {
    return "spans";
  }

  @Override
  protected CompletableResultCode doExport(Collection<SpanData> data) {
    return wrapped.export(data);
  }

  @Override
  protected SignalSerializer<SpanData> getSerializer() {
    return spanDeserializer;
  }

  @Override
  public CompletableResultCode flush() {
    try {
      exportStoredBatch(10, TimeUnit.SECONDS);
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    }
    return wrapped.flush();
  }

  private static class SpanDeserializer implements SignalSerializer<SpanData> {

    @Override
    public byte[] serialize(Collection<SpanData> spanData) {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("{\"resourceSpans\":[".getBytes(StandardCharsets.UTF_8));
        for (ResourceSpansMarshaler marshaler : ResourceSpansMarshaler.create(spanData)) {
          baos.reset();
          marshaler.writeJsonTo(baos);
        }
        baos.write("]}".getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
      } catch (IOException e) {
        throw new IllegalStateException("Serialization error", e);
      }
    }

    @Override
    public List<SpanData> deserialize(byte[] source) {
      ResourceSpans.Builder builder = ResourceSpans.newBuilder();
      try {
        JsonFormat.parser().merge(new String(source), builder);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }
      ResourceSpans resourceSpans = builder.build();

      List<SpanData> spanData = new ArrayList<>();

      ResourceBuilder resourceBuilder = Resource.builder();
      resourceBuilder.putAll(toAttributes(resourceSpans.getResource().getAttributesList()));
      // TODO: map other resourceFields
      Resource resource = resourceBuilder.build();

      for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
        InstrumentationScopeInfoBuilder scopeBuilder =
            InstrumentationScopeInfo.builder(scopeSpans.getScope().getName());
        scopeBuilder.setVersion(scopeSpans.getScope().getVersion());
        scopeBuilder.setAttributes(toAttributes(scopeSpans.getScope().getAttributesList()));
        // TODO: map other scope fields
        InstrumentationScopeInfo scope = scopeBuilder.build();

        for (Span span : scopeSpans.getSpansList()) {
          spanData.add(
              TestSpanData.builder()
                  .setResource(resource)
                  .setInstrumentationScopeInfo(scope)
                  .setStartEpochNanos(span.getStartTimeUnixNano())
                  .setEndEpochNanos(span.getEndTimeUnixNano())
                  .setSpanContext(
                      SpanContext.create(
                          TraceId.fromBytes(toHex(span.getTraceId()).toByteArray()),
                          SpanId.fromBytes(toHex(span.getSpanId()).toByteArray()),
                          TraceFlags.getDefault(), // TODO: map trace flags
                          TraceState.getDefault() // TODO: map trace state
                          ))
                  .setName(span.getName())
                  .setAttributes(toAttributes(span.getAttributesList()))
                  .setKind(toSpanKind(span.getKind()))
                  .setStatus(
                      StatusData.create(
                          toStatusCode(span.getStatus().getCode()), span.getStatus().getMessage()))
                  .setHasEnded(true)
                  .setTotalAttributeCount(
                      span.getAttributesCount() + span.getDroppedAttributesCount())
                  .setTotalRecordedEvents(span.getEventsCount() + span.getDroppedEventsCount())
                  .setTotalRecordedLinks(span.getLinksCount() + span.getDroppedLinksCount())
                  .build()); // TODO: map other span fields (e.g. events, links)
        }
      }

      resourceSpans.getResource().getAttributesList();

      return spanData;
    }

    private static Attributes toAttributes(List<KeyValue> keyValues) {
      AttributesBuilder builder = Attributes.builder();
      for (KeyValue keyValue : keyValues) {
        AnyValue value = keyValue.getValue();
        if (value.hasStringValue()) {
          builder.put(keyValue.getKey(), value.getStringValue());
          continue;
        }
        if (value.hasBoolValue()) {
          builder.put(keyValue.getKey(), value.getBoolValue());
          continue;
        }
        if (value.hasIntValue()) {
          builder.put(keyValue.getKey(), value.getIntValue());
          continue;
        }
        if (value.hasDoubleValue()) {
          builder.put(keyValue.getKey(), value.getDoubleValue());
          continue;
        }
        // TODO: add support for arrays
      }
      return builder.build();
    }

    private static SpanKind toSpanKind(Span.SpanKind spanKind) {
      switch (spanKind) {
        case SPAN_KIND_CLIENT:
          return SpanKind.CLIENT;
        case SPAN_KIND_SERVER:
          return SpanKind.SERVER;
        case SPAN_KIND_PRODUCER:
          return SpanKind.PRODUCER;
        case SPAN_KIND_CONSUMER:
          return SpanKind.CONSUMER;
        case SPAN_KIND_INTERNAL:
          return SpanKind.INTERNAL;
        default:
          throw new IllegalStateException("Unrecognized");
      }
    }

    private static StatusCode toStatusCode(Status.StatusCode statusCode) {
      switch (statusCode) {
        case STATUS_CODE_OK:
          return StatusCode.OK;
        case STATUS_CODE_ERROR:
          return StatusCode.ERROR;
        default:
          return StatusCode.UNSET;
      }
    }

    @SuppressWarnings("UnusedMethod")
    private static ByteString toHex(ByteString hexReadAsBase64) {
      String hex =
          Base64.getEncoder()
              .encodeToString(hexReadAsBase64.toByteArray())
              .toLowerCase(Locale.ROOT);
      return ByteString.copyFrom(OtelEncodingUtils.bytesFromBase16(hex, hex.length()));
    }
  }
}
