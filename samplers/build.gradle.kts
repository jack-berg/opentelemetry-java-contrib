plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler which makes its decision based on semantic attributes values"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk:1.31.0-SNAPSHOT")
  api("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.31.0-SNAPSHOT")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:1.31.0-SNAPSHOT")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-incubator:1.31.0-alpha-SNAPSHOT")
}
