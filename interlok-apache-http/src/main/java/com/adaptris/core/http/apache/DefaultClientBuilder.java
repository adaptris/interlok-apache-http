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
package com.adaptris.core.http.apache;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageProducer;
import com.adaptris.util.TimeInterval;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;

/**
 * default {@link HttpClientBuilderConfigurator} instance
 *
 * @config default-apache-http-client-builder
 *
 */
@DisplayOrder(order =
{
    "httpProxy", "allowRedirect", "connectTimeout", "readTimeout"
})
@XStreamAlias("default-apache-http-client-builder")
@NoArgsConstructor
public class DefaultClientBuilder implements HttpClientBuilderConfigurator {

  /**
   * Explicitly configurd proxy server.
   * <p>Follows the form {@code scheme://host:port} or more simply {@code host:port} and if it ends up being
   * just a {@code :} then is assumed that no proxy is required (this is to make it more convenient to migrate
   * configuration through environments, some of which may require a proxy, some not.
   * </p>
   */
  @Getter
  @Setter
  @AdvancedConfig
  private String httpProxy;
  /**
   * Allow redirection.
   * <p>Defaults to true if not explicitly specified, and uses a {@code LaxRedirectStrategy} which means that HTTP
   * redirects are allows for POST and PUT operations. While this is against the HTTP specific, it's more
   * convenient</p>
   */
  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldDefault(value = "true")
  private Boolean allowRedirect;
  /** The connect timeout.
   *
   */
  @Valid
  @AdvancedConfig
  @Getter
  @Setter
  private TimeInterval connectTimeout;
  /** The read timeout.
   * <p>
   * Note that any read timeout will be overridden by the timeout value passed in via the
   * {{@link AdaptrisMessageProducer#request(AdaptrisMessage, long)} method, provided it differs from
   * {@code RequestReplyProducerImp#defaultTimeout()}. Apache HTTP calls this the socket timeout in their documentation.
   * </p>
   */
  @Valid
  @AdvancedConfig
  @Getter
  @Setter
  private TimeInterval readTimeout;

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder, long timeout) throws Exception {
    return configure(customiseTimeouts(builder, timeout));
  }

  @Override
  public HttpClientBuilder configure(HttpClientBuilder builder) throws Exception {
    if (!handleRedirection()) {
      builder.disableRedirectHandling();
    } else {
      builder.setRedirectStrategy(new LaxRedirectStrategy());
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
   * @param builder the builder
   * @param timeout the timeout specified by
   *        {@code RequestReplyProducerImp#doRequest(AdaptrisMessage, String, long)}
   * @return the builder.
   */
  protected HttpClientBuilder customiseTimeouts(HttpClientBuilder builder, long timeout) {
    RequestConfig.Builder requestCfg = RequestConfig.custom();
    if (getConnectTimeout() != null) {
      requestCfg.setConnectTimeout(Long.valueOf(getConnectTimeout().toMilliseconds()).intValue());
      requestCfg.setConnectionRequestTimeout(Long.valueOf(getConnectTimeout().toMilliseconds()).intValue());
    } else if (timeout >= 0) {
      requestCfg.setConnectTimeout(Long.valueOf(timeout).intValue());
      requestCfg.setConnectionRequestTimeout(Long.valueOf(timeout).intValue());
    }
    if (getReadTimeout() != null) {
      requestCfg.setSocketTimeout(Long.valueOf(getReadTimeout().toMilliseconds()).intValue());
      builder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Long.valueOf(getReadTimeout().toMilliseconds()).intValue()).build());
    } else if (timeout >= 0) {
      requestCfg.setSocketTimeout(Long.valueOf(timeout).intValue());
      builder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Long.valueOf(timeout).intValue()).build());
    }
    builder.setDefaultRequestConfig(requestCfg.build());
    return builder;
  }

  protected boolean handleRedirection() {
    return BooleanUtils.toBooleanDefaultIfNull(getAllowRedirect(), true);
  }

  @SuppressWarnings("unchecked")
  public <T extends DefaultClientBuilder> T withAllowRedirect(Boolean b) {
    setAllowRedirect(b);
    return (T) this;
  }


  @SuppressWarnings("unchecked")
  public <T extends DefaultClientBuilder> T withProxy(String b) {
    setHttpProxy(b);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends DefaultClientBuilder> T withConnectTimeout(TimeInterval b) {
    setConnectTimeout(b);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends DefaultClientBuilder> T withReadTimeout(TimeInterval b) {
    setReadTimeout(b);
    return (T) this;
  }
}
