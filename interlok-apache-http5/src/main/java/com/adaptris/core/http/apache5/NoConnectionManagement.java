/*
 * Copyright 2018 Adaptris Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.adaptris.core.http.apache5;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;

/**
 * <p>
 * Remove any connection management from the underlying {@code HttpClientBuilder} instance.
 * </p>
 * This effectively sets the following properties.
 * <ul>
 * <li>{@code HttpClientBuilder#setConnectionManagerShared(boolean)} = false</li>
 * <li>{@code HttpClientBuilder#setConnectionManager(HttpClientConnectionManager)} = {@code BasicHttpClientConnectionManager}</li>
 * <li>{@code HttpClientBuilder#setConnectionReuseStrategy(ConnectionReuseStrategy)} =
 * {@code NoConnectionReuseStrategy#INSTANCE}</li>
 * </ul>
 *
 * @config no-connection-management-apache-http-client-builder
 *
 */
@XStreamAlias("no-connection-management-apache-http5-client-builder")
public class NoConnectionManagement implements HttpClientBuilderConfigurator {

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    HttpClientBuilder result = builder;
    result.setConnectionManagerShared(false);
    result.setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE);
    result.setConnectionManager(new BasicHttpClientConnectionManager());
    return result;
  }

}
