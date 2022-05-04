/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.httpurlconnection;

import io.opentelemetry.context.propagation.TextMapSetter;
import java.net.HttpURLConnection;
import java.util.Arrays;

enum RequestPropertySetter implements TextMapSetter<HttpURLConnection> {
  INSTANCE;

  @Override
  public void set(HttpURLConnection carrier, String key, String value) {
    System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()).replace( ',', '\n' ));
    System.out.println("set request property key: " + key + " value: " + value);
    carrier.setRequestProperty(key, value);
  }
}
