/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.processor;

import io.opentelemetry.exporter.otlp.internal.OtlpSpanExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.lang.reflect.Field;
import java.util.List;

public class TracerProviderCustomizer implements AutoConfigurationCustomizerProvider {
  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addTracerProviderCustomizer(
        (sdkTracerProviderBuilder, configProperties) -> {
          if (configProperties.getBoolean("otel.ltbp.enabled", true)) {
            unsetSpanProcessors(sdkTracerProviderBuilder);
            sdkTracerProviderBuilder.addSpanProcessor(
                LocalTraceBatchProcessor.create(
                    unused -> true,
                    new OtlpSpanExporterProvider().createExporter(configProperties)));
          }
          return sdkTracerProviderBuilder;
        });
  }

  @SuppressWarnings("unchecked")
  private static void unsetSpanProcessors(SdkTracerProviderBuilder builder) {
    try {
      Field spanProcessors = SdkTracerProviderBuilder.class.getDeclaredField("spanProcessors");
      spanProcessors.setAccessible(true);
      List<SpanProcessor> list = (List<SpanProcessor>) spanProcessors.get(builder);
      list.clear();
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
