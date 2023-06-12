/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.data.Exemplar;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@CompiledJson
public final class HistogramDataPoint extends DataPoint {

  @Nullable
  @JsonAttribute(name = "count")
  public String count;

  @Nullable
  @JsonAttribute(name = "sum")
  public Double sum;

  @Nullable
  @JsonAttribute(name = "min")
  public Double min;

  @Nullable
  @JsonAttribute(name = "max")
  public Double max;

  @JsonAttribute(name = "bucketCounts")
  public List<String> counts = new ArrayList<>();

  @JsonAttribute(name = "explicitBounds")
  public List<Double> boundaries = new ArrayList<>();

  @JsonAttribute(name = "exemplars")
  public List<Exemplar> exemplars = new ArrayList<>();
}
