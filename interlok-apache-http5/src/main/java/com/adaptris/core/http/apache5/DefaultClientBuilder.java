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

import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.validation.Valid;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageProducer;
import com.adaptris.core.RequestReplyProducerImp;
import com.adaptris.util.TimeInterval;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * default {@link HttpClientBuilderConfigurator} instance
 *
 * @config default-apache-http-client-builder
 *
 */
@DisplayOrder(order = { "httpProxy", "allowRedirect", "connectTimeout", "readTimeout" })
@XStreamAlias("default-apache-http5-client-builder")
public class DefaultClientBuilder implements HttpClientBuilderConfigurator {

  @AdvancedConfig
  private String httpProxy;
  @AdvancedConfig
  @InputFieldDefault(value = "true")
  private Boolean allowRedirect;
  @Valid
  @AdvancedConfig
  private TimeInterval connectTimeout;
  @Valid
  @AdvancedConfig
  private TimeInterval readTimeout;

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder, long timeout) throws Exception {
    customiseTimeouts(builder, timeout);
    return configure(builder);
  }

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    if (!handleRedirection()) {
      builder.disableRedirectHandling();
    } else {
      builder.setRedirectStrategy(new DefaultRedirectStrategy());
    }
    String httpProxy = getHttpProxy();
    if (!isBlank(httpProxy) && !":".equals(httpProxy)) {
      builder.setProxy(HttpHost.create(httpProxy));
    }
    return builder.setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider()).useSystemProperties();
  }

  /**
   * Customise any timeouts as required.
   *
   * @param builder
   *          the builder
   * @param timeout
   *          the timeout specified by {@link RequestReplyProducerImp#doRequest(AdaptrisMessage, String, long)}
   * @return the builder.
   */
  protected HttpClientBuilder customiseTimeouts(HttpClientBuilder builder, long timeout) {
    BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager();
    RequestConfig.Builder requestCfg = RequestConfig.custom();
    if (getConnectTimeout() != null) {
      requestCfg.setConnectTimeout(Timeout.ofMilliseconds(getConnectTimeout().toMilliseconds()));
      requestCfg.setConnectionRequestTimeout(Timeout.ofMilliseconds(getConnectTimeout().toMilliseconds()));
    } else if (timeout >= 0) {
      requestCfg.setConnectTimeout(Timeout.ofMilliseconds(timeout));
      requestCfg.setConnectionRequestTimeout(Timeout.ofMilliseconds(timeout));
    }
    if (getReadTimeout() != null) {
      requestCfg.setResponseTimeout(Timeout.ofMilliseconds(getReadTimeout().toMilliseconds()));
      connectionManager
          .setSocketConfig(SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(getReadTimeout().toMilliseconds())).build());
    } else if (timeout >= 0) {
      requestCfg.setResponseTimeout(Timeout.ofMilliseconds(timeout));
      connectionManager.setSocketConfig(SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(timeout)).build());
    }
    builder.setDefaultRequestConfig(requestCfg.build());
    builder.setConnectionManager(connectionManager);
    return builder;
  }

  /**
   * Specify whether to automatically handle redirection.
   *
   * @param b
   *          true or false.
   */
  public void setAllowRedirect(Boolean b) {
    allowRedirect = b;
  }

  protected boolean handleRedirection() {
    return BooleanUtils.toBooleanDefaultIfNull(getAllowRedirect(), true);
  }

  /**
   * Get the handle redirection flag.
   *
   * @return true or false.
   */
  public Boolean getAllowRedirect() {
    return allowRedirect;
  }

  public <T extends DefaultClientBuilder> T withAllowRedirect(Boolean b) {
    setAllowRedirect(b);
    return (T) this;
  }

  /**
   * @return the httpProxy
   */
  public String getHttpProxy() {
    return httpProxy;
  }

  /**
   * Explicitly configure a proxy server.
   *
   * @param proxy
   *          the httpProxy to generally {@code scheme://host:port} or more simply {@code host:port}
   */
  public void setHttpProxy(String proxy) {
    httpProxy = proxy;
  }

  public <T extends DefaultClientBuilder> T withProxy(String b) {
    setHttpProxy(b);
    return (T) this;
  }

  public TimeInterval getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Set the connect timeout.
   *
   * @param t
   *          the timeout.
   */
  public void setConnectTimeout(TimeInterval t) {
    connectTimeout = t;
  }

  public <T extends DefaultClientBuilder> T withConnectTimeout(TimeInterval b) {
    setConnectTimeout(b);
    return (T) this;
  }

  public TimeInterval getReadTimeout() {
    return readTimeout;
  }

  /**
   * Set the read timeout.
   * <p>
   * Note that any read timeout will be overridden by the timeout value passed in via the
   * {{@link AdaptrisMessageProducer#request(AdaptrisMessage, long)} method, provided it differs from
   * {@link RequestReplyProducerImp#defaultTimeout()}. Apache HTTP calls this the socket timeout in their documentation.
   * </p>
   *
   * @param t
   *          the timeout.
   */
  public void setReadTimeout(TimeInterval t) {
    readTimeout = t;
  }

  public <T extends DefaultClientBuilder> T withReadTimeout(TimeInterval b) {
    setReadTimeout(b);
    return (T) this;
  }

}
