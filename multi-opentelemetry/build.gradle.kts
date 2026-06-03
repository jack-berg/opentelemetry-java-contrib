plugins {
  id("otel.java-conventions")
  // id("otel.publish-conventions")
}

description = "TODO"
otelJava.moduleName.set("io.opentelemetry.contrib.multiopentelemetry")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
}
