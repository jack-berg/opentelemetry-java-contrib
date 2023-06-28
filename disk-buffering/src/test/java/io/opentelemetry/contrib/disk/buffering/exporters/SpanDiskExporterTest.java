/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpanDiskExporterTest {
  private SpanExporter wrapped;
  private SpanDiskExporter exporter;
  @TempDir File rootDir;

  @BeforeEach
  public void setUp() {
    wrapped = mock();
    exporter = new SpanDiskExporter(wrapped, rootDir, TestData.getDefaultConfiguration());
  }

  @Test
  void endToEnd() throws InterruptedException {
    when(wrapped.flush()).thenReturn(CompletableResultCode.ofSuccess());
    when(wrapped.export(any())).thenReturn(CompletableResultCode.ofSuccess());

    List<SpanData> spanData =
        Collections.singletonList(
            TestSpanData.builder()
                .setHasEnded(true)
                .setSpanContext(
                    SpanContext.create(
                        TraceId.fromBytes(
                            new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4}),
                        SpanId.fromBytes(new byte[] {0, 0, 0, 0, 4, 3, 2, 1}),
                        TraceFlags.getDefault(),
                        TraceState.getDefault()))
                .setStartEpochNanos(100)
                .setEndEpochNanos(100 + 1000)
                .setStatus(StatusData.ok())
                .setName("testSpan1")
                .setKind(SpanKind.INTERNAL)
                .setAttributes(Attributes.of(stringKey("animal"), "cat", longKey("lives"), 9L))
                .setTotalAttributeCount(2)
                .setTotalRecordedEvents(1)
                .setTotalRecordedLinks(0)
                .setInstrumentationScopeInfo(
                    InstrumentationScopeInfo.builder("instrumentation")
                        .setVersion("1")
                        .setAttributes(Attributes.builder().put("key", "value").build())
                        .build())
                .setResource(Resource.getDefault())
                .build());

    exporter.export(spanData).join(10, TimeUnit.SECONDS);
    Thread.sleep(2000);
    exporter.flush();

    verify(wrapped, times(1)).export(spanData);
  }

  @Test
  public void verifyStorageFolderName() {
    assertEquals("spans", exporter.getStorageFolderName());
  }

  @Test
  public void callWrappedWhenDoingExport() {
    List<SpanData> data = Collections.emptyList();
    CompletableResultCode result = CompletableResultCode.ofSuccess();
    doReturn(result).when(wrapped).export(data);

    assertEquals(result, exporter.doExport(data));

    verify(wrapped).export(data);
  }

  @Test
  @Disabled
  public void verifySerializer() {
    assertEquals(SignalSerializer.ofSpans(), exporter.getSerializer());
  }

  @Test
  public void onFlush_flushWrappedExporter() {
    exporter.flush();

    verify(wrapped).flush();
  }
}
