package io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data;

import com.dslplatform.json.CompiledJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints.SummaryDataPoint;

@CompiledJson
public final class Summary extends DataJson<SummaryDataPoint> {}