package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data.base.AggregationTemporalityBuilder;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data.base.DataBuilder;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;

@AutoValue
public abstract class HistogramDataImpl implements HistogramData {

  public static Builder builder() {
    return new AutoValue_HistogramDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder
      implements DataBuilder<HistogramPointData, Builder>, AggregationTemporalityBuilder<Builder> {
    public abstract HistogramDataImpl build();
  }
}