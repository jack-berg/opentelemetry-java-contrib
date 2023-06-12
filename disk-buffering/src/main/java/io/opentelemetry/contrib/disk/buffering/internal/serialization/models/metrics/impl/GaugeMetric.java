/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.impl;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.MetricDataJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.DataJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.Gauge;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class GaugeMetric extends MetricDataJson {
  public static final String DATA_NAME = "gauge";

  @Nullable
  @JsonAttribute(name = DATA_NAME)
  public Gauge gauge;

  @Override
  public void setData(DataJson<?> data) {
    gauge = (Gauge) data;
  }

  @Nullable
  @Override
  public DataJson<?> getData() {
    return gauge;
  }
}
