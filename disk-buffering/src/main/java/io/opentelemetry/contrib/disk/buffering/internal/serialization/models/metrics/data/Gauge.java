/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data;

import com.dslplatform.json.CompiledJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.NumberDataPoint;

@CompiledJson
public final class Gauge extends DataJson<NumberDataPoint> {}