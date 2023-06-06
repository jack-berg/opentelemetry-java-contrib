package io.opentelemetry.contrib.exporters.storage.serialization.spans;

import com.dslplatform.json.JsonAttribute;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nullable;

public final class LinkDataJson {

  @Nullable
  @JsonAttribute(name = "traceId")
  public String traceId;

  @Nullable
  @JsonAttribute(name = "spanId")
  public String spanId;

  @JsonAttribute(name = "attributes")
  public Attributes attributes = Attributes.empty();

  @Nullable
  @JsonAttribute(name = "droppedAttributesCount")
  public Integer droppedAttributesCount;
}