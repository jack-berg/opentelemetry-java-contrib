package io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.DataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.Histogram;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.MetricDataJson;
import javax.annotation.Nullable;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class HistogramMetric extends MetricDataJson {
  public static final String DATA_NAME = "histogram";

  @Nullable
  @JsonAttribute(name = DATA_NAME)
  public Histogram histogram;

  @Override
  public void setData(DataJson<?> data) {
    histogram = (Histogram) data;
  }

  @Nullable
  @Override
  public DataJson<?> getData() {
    return histogram;
  }
}