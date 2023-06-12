/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.impl;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.MetricDataJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.DataJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.Sum;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class SumMetric extends MetricDataJson {
  public static final String DATA_NAME = "sum";

  @Nullable
  @JsonAttribute(name = DATA_NAME)
  public Sum sum;

  @Override
  public void setData(DataJson<?> data) {
    sum = (Sum) data;
  }

  @Nullable
  @Override
  public DataJson<?> getData() {
    return sum;
  }
}
