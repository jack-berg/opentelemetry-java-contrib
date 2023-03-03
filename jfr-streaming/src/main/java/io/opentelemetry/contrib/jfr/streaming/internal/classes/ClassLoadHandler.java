/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.streaming.internal.classes;

import io.opentelemetry.contrib.jfr.streaming.JfrFeature;
import io.opentelemetry.contrib.jfr.streaming.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public final class ClassLoadHandler implements RecordedEventHandler {

  public ClassLoadHandler() {
  }

  @Override
  public void accept(RecordedEvent ev) {
    System.out.print(ev);
  }

  @Override
  public String getEventName() {
    return "jdk.ClassLoad";
  }

  @Override
  public JfrFeature getFeature() {
    return JfrFeature.CLASS_LOAD_METRICS;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }

  @Override
  public void close() {
  }
}
