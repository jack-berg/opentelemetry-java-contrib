/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.common.ResourceSignals;
import java.util.ArrayList;
import java.util.List;

@CompiledJson(objectFormatPolicy = CompiledJson.ObjectFormatPolicy.EXPLICIT)
public final class ResourceLogs extends ResourceSignals<ScopeLogs> {

  @JsonAttribute(name = "scopeLogs")
  public List<ScopeLogs> scopeLogs = new ArrayList<>();

  @Override
  public void addScopeSignalsItem(ScopeLogs item) {
    scopeLogs.add(item);
  }

  @Override
  public List<ScopeLogs> getScopeSignals() {
    return scopeLogs;
  }
}