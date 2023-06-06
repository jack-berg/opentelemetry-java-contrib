package io.opentelemetry.contrib.disk.buffer.testutils;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;

@SuppressWarnings("unchecked")
public final class TestData {
  public static final String TRACE_ID = "b535b3b5232b5dabced5b0ab8037eb78";
  public static final String SPAN_ID = "f3fc364fb6b77cff";
  public static final String PARENT_SPAN_ID = "d3fc364fb6b77cfa";
  public static final Attributes ATTRIBUTES =
      Attributes.builder()
          .put("bear", "mya")
          .put("warm", true)
          .put("temperature", 30)
          .put("length", 1.2)
          .put("colors", "red", "blue")
          .put("conditions", false, true)
          .put("scores", 0L, 1L)
          .put("coins", 0.01, 0.05, 0.1)
          .build();

  public static final Resource RESOURCE_FULL =
      Resource.create(
          Attributes.builder().put("resourceAttr", "resourceAttrValue").build(),
          "resourceSchemaUrl");

  public static final SpanContext SPAN_CONTEXT =
      SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
  public static final SpanContext PARENT_SPAN_CONTEXT =
      SpanContext.create(
          TRACE_ID, PARENT_SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
  public static final InstrumentationScopeInfo INSTRUMENTATION_SCOPE_INFO_FULL =
      InstrumentationScopeInfo.builder("Instrumentation scope name")
          .setVersion("1.2.3")
          .setSchemaUrl("instrumentationScopeInfoSchemaUrl")
          .setAttributes(
              Attributes.builder()
                  .put("instrumentationScopeInfoAttr", "instrumentationScopeInfoAttrValue")
                  .build())
          .build();

  private TestData() {}
}