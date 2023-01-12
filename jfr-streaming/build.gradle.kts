plugins {
  id("otel.java-conventions")
}

dependencies {
  implementation("io.opentelemetry:opentelemetry-api")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

tasks {
  withType(JavaCompile::class) {
    options.release.set(17)
  }

  withType<Javadoc>().configureEach {
    with(options as StandardJavadocDocletOptions) {
      source = "17"
    }
  }

  test {
    useJUnitPlatform()
  }
}

testing {
  suites {
    val testShenandoah by registering(JvmTestSuite::class) {
      dependencies {
        implementation("io.opentelemetry:opentelemetry-sdk-testing")
      }

      targets {
        all {
          testTask {
            jvmArgs = listOf("-XX:+UnlockExperimentalVMOptions", "-XX:+UseShenandoahGC")
          }
        }
      }
    }
  }
}

tasks {
  check {
    dependsOn(testing.suites)
  }
}
