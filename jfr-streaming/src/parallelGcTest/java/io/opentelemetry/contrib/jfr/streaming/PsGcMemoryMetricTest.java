/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.streaming;

import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_ACTION;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_GC;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_PS_EDEN_SPACE;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_PS_OLD_GEN;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.ATTR_PS_SURVIVOR_SPACE;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.END_OF_MAJOR_GC;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.END_OF_MINOR_GC;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_COMMITTED;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_GC_DURATION;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_MEMORY;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_DESCRIPTION_MEMORY_LIMIT;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_NAME_MEMORY;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.METRIC_NAME_MEMORY_AFTER;
import static io.opentelemetry.contrib.jfr.streaming.internal.Constants.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PsGcMemoryMetricTest {

  @RegisterExtension
  JfrExtension jfrExtension =
      new JfrExtension(
          builder ->
              builder
                  .disableAllFeatures()
                  .enableFeature(JfrFeature.GC_DURATION_METRICS)
                  .enableFeature(JfrFeature.MEMORY_POOL_METRICS));

  @Test
  void shouldHaveMemoryUsageMetrics() {
    System.gc();
    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY)
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY)
                .satisfies(
                    data ->
                        assertThat(data.getLongSumData().getPoints())
                            .anyMatch(p -> p.getAttributes().equals(ATTR_PS_EDEN_SPACE))
                            .anyMatch(p -> p.getAttributes().equals(ATTR_PS_SURVIVOR_SPACE))
                            .anyMatch(p -> p.getAttributes().equals(ATTR_PS_OLD_GEN))),
        metric ->
            metric
                .hasName(METRIC_NAME_MEMORY_AFTER)
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY_AFTER)
                .satisfies(
                    data ->
                        assertThat(data.getLongSumData().getPoints())
                            .anyMatch(p -> p.getAttributes().equals(ATTR_PS_EDEN_SPACE))
                            .anyMatch(p -> p.getAttributes().equals(ATTR_PS_SURVIVOR_SPACE))
                            .anyMatch(p -> p.getAttributes().equals(ATTR_PS_OLD_GEN))));
  }

  @Test
  void shouldHaveMemoryLimitMetrics() {
    System.gc();
    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.memory.limit")
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_MEMORY_LIMIT)
                .satisfies(
                    metricData -> {
                      SumData<?> sumData = metricData.getLongSumData();
                      assertThat(sumData.getPoints())
                          .anyMatch(p -> p.getAttributes().equals(ATTR_PS_EDEN_SPACE))
                          .anyMatch(p -> p.getAttributes().equals(ATTR_PS_SURVIVOR_SPACE))
                          .anyMatch(p -> p.getAttributes().equals(ATTR_PS_OLD_GEN));
                    }));
  }

  @Test
  void shouldHaveMemoryCommittedMetrics() {
    System.gc();
    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.memory.committed")
                .hasUnit(BYTES)
                .hasDescription(METRIC_DESCRIPTION_COMMITTED)
                .satisfies(
                    metricData -> {
                      SumData<?> sumData = metricData.getLongSumData();
                      assertThat(sumData.getPoints())
                          .anyMatch(p -> p.getAttributes().equals(ATTR_PS_EDEN_SPACE))
                          .anyMatch(p -> p.getAttributes().equals(ATTR_PS_SURVIVOR_SPACE))
                          .anyMatch(p -> p.getAttributes().equals(ATTR_PS_OLD_GEN));
                    }));
  }

  @Test
  void shouldHaveGCDurationMetrics() {
    // TODO: Need a reliable way to test old and young gen GC in isolation.
    System.gc();
    jfrExtension.waitAndAssertMetrics(
        metric ->
            metric
                .hasName("process.runtime.jvm.gc.duration")
                .hasUnit(MILLISECONDS)
                .hasDescription(METRIC_DESCRIPTION_GC_DURATION)
                .satisfies(
                    metricData -> {
                      HistogramData data = metricData.getHistogramData();
                      assertThat(data.getPoints())
                          .map(HistogramPointData.class::cast)
                          .anyMatch(
                              p ->
                                  p.getSum() > 0
                                      && (p.getAttributes()
                                              .equals(
                                                  Attributes.of(
                                                      ATTR_GC,
                                                      "PS Scavenge",
                                                      ATTR_ACTION,
                                                      END_OF_MINOR_GC))
                                          || p.getAttributes()
                                              .equals(
                                                  Attributes.of(
                                                      ATTR_GC,
                                                      "PS MarkSweep",
                                                      ATTR_ACTION,
                                                      END_OF_MAJOR_GC))));
                    }));
  }
}
