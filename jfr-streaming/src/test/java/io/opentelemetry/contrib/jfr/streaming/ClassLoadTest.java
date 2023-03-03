/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.streaming;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.reflect.ClassPath;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordingStream;
import org.junit.jupiter.api.Test;

class ClassLoadTest {

  @Test
  void jfr() throws InterruptedException, ClassNotFoundException {
    RecordingStream recordingStream = new RecordingStream();
    // recordingStream.enable("jdk.ClassDefine");
    recordingStream.enable("jdk.ClassLoad");
    try {
      Set<URL> jarUrls = new HashSet<>();
      recordingStream.onEvent(event -> {
        RecordedClass loadedClass = event.getValue("loadedClass");
        CodeSource codeSource;
        try {
          codeSource = Class.forName(loadedClass.getName()).getProtectionDomain().getCodeSource();
        } catch (ClassNotFoundException e) {
          System.out.println("Error obtaining class: " + e.getMessage());
          return;
        }
        if (codeSource == null) {
          return;
        }
        URL classJarUrl = codeSource.getLocation();
        if (jarUrls.add(classJarUrl)) {
          Map.Entry<String, String> entry = hashJar(classJarUrl);
          System.out.println(entry.getKey() + ": " + entry.getValue());
        }
      });
      recordingStream.startAsync();
      Thread.sleep(1000);
      Class.forName("io.opentelemetry.api.GlobalOpenTelemetry");
      Class.forName("io.opentelemetry.sdk.OpenTelemetrySdk");
      Thread.sleep(2000);
    } finally {
      recordingStream.close();
    }
  }

  @Test
  void guava() throws IOException {
    long start = System.currentTimeMillis();
    ClassPath classPath = ClassPath.from(ClassLoadTest.class.getClassLoader());
    Set<ClassPath.ClassInfo> classes = classPath.getAllClasses();
    Set<URL> jarUrls = new HashSet<>();
    classes.forEach(classinfo -> {
      try {
        jarUrls.add(classinfo.load().getProtectionDomain().getCodeSource().getLocation());
      } catch (NoClassDefFoundError e) {
      }
    });

    Map<String, String> jarShortNameToHash = new HashMap<>();
    for (URL jarUrl : jarUrls) {
      try {
        Map.Entry<String, String> entry = hashJar(jarUrl);
        jarShortNameToHash.put(entry.getKey(), entry.getValue());
      } catch (RuntimeException e) {
        System.out.print(e.getMessage());
      }
    }

    jarShortNameToHash.forEach((k, v) -> System.out.println(k + ": " + v));
    System.out.println("Time to compute: " + (System.currentTimeMillis() - start));
  }

  private Map.Entry<String, String> hashJar(URL jarUrl) {
    try {
      File jarFile = new File(jarUrl.getFile());
      byte[] bytes = Files.asByteSource(jarFile).read();
      return Map.entry(jarFile.getName(), Hashing.sha256().hashBytes(bytes).toString());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read jar " + jarUrl.getFile(), e);
    }
  }
}
