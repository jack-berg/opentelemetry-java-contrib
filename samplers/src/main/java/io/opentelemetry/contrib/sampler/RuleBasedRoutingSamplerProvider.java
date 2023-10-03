/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.ExtendedConfigProperties;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;
import java.util.NoSuchElementException;

public class RuleBasedRoutingSamplerProvider implements ConfigurableSamplerProvider {

  @Override
  public Sampler createSampler(ConfigProperties config) {
    if (!(config instanceof ExtendedConfigProperties)) {
      throw new ConfigurationException("Only support with file configuration");
    }

    String fallbackSamplerString = config.getString("fallback", "always_on");
    Sampler fallbackSampler;
    if (fallbackSamplerString.equals("always_on")) {
      fallbackSampler = Sampler.alwaysOn();
    } else if (fallbackSamplerString.equals("always_off")) {
      fallbackSampler = Sampler.alwaysOff();
    } else {
      throw new IllegalArgumentException("fallback must be always_on or always_off");
    }

    String spanKindString = config.getString("span_kind", "SERVER");
    SpanKind spanKind;
    try {
      spanKind = SpanKind.valueOf(config.getString("span_kind", "SERVER"));
    } catch (NoSuchElementException e) {
      throw new ConfigurationException("Invalid span_kind: " + spanKindString, e);
    }

    RuleBasedRoutingSamplerBuilder builder =
        RuleBasedRoutingSampler.builder(spanKind, fallbackSampler);

    List<ExtendedConfigProperties> rules =
        ((ExtendedConfigProperties) config).getListConfigProperties("drop_rules");
    if (rules == null || rules.isEmpty()) {
      throw new ConfigurationException("drop_rules is required");
    }
    for (ExtendedConfigProperties rule : rules) {
      String attribute = rule.getString("attribute");
      String pattern = rule.getString("pattern");
      if (attribute == null || pattern == null) {
        throw new ConfigurationException("drop_rule entries require attribute and pattern fields");
      }
      builder.drop(AttributeKey.stringKey(attribute), pattern);
    }

    return builder.build();
  }

  @Override
  public String getName() {
    return "rule_based_routing_sampler";
  }
}
