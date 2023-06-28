/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.storage.files.DefaultTemporaryFileProvider;
import io.opentelemetry.contrib.disk.buffering.storage.files.TemporaryFileProvider;

public final class TestData {

  public static final long MAX_FILE_AGE_FOR_WRITE_MILLIS = 1000;
  public static final long MIN_FILE_AGE_FOR_READ_MILLIS = MAX_FILE_AGE_FOR_WRITE_MILLIS + 500;
  public static final long MAX_FILE_AGE_FOR_READ_MILLIS = 10_000;
  public static final int MAX_FILE_SIZE = 10000;
  public static final int MAX_FOLDER_SIZE = 30000;

  public static StorageConfiguration getDefaultConfiguration() {
    return getConfiguration(DefaultTemporaryFileProvider.INSTANCE);
  }

  public static StorageConfiguration getConfiguration(TemporaryFileProvider fileProvider) {
    return StorageConfiguration.builder()
        .setMaxFileAgeForWriteMillis(MAX_FILE_AGE_FOR_WRITE_MILLIS)
        .setMinFileAgeForReadMillis(MIN_FILE_AGE_FOR_READ_MILLIS)
        .setMaxFileAgeForReadMillis(MAX_FILE_AGE_FOR_READ_MILLIS)
        .setMaxFileSize(MAX_FILE_SIZE)
        .setMaxFolderSize(MAX_FOLDER_SIZE)
        .setTemporaryFileProvider(fileProvider)
        .build();
  }

  private TestData() {}
}
