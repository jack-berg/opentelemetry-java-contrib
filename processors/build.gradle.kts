plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

description = "Contrib Processors"
otelJava.moduleName.set("io.opentelemetry.contrib.processor")

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-exporter-otlp")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

tasks {
  jar {
    enabled = false
  }

  assemble {
    dependsOn("shadowJar")
  }

  shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("local-trace-batch-processor.jar")
  }

  test {
    dependsOn("shadowJar")
  }
}
